package org.kingdoms.versioning

import org.kingdoms.constants.DataStringRepresentation
import org.kingdoms.utils.internal.enumeration.Enums
import java.util.*

interface VersionBase<T : VersionBase<T>> : Comparable<T>, DataStringRepresentation {
    fun supersedes(other: T): Boolean = compareTo(other) > 0
    fun precedes(other: T): Boolean = compareTo(other) < 0
    fun canBeComparedTo(other: T): Boolean
    fun getFriendlyString(short: Boolean): String

    override fun hashCode(): Int
    override fun equals(other: Any?): Boolean
}

interface VersionPart : VersionBase<VersionPart> {
    override fun supersedes(other: VersionPart): Boolean = compareTo(other) > 0
    override fun precedes(other: VersionPart): Boolean = compareTo(other) < 0
    override fun canBeComparedTo(other: VersionPart): Boolean
    override fun getFriendlyString(short: Boolean): String

    /**
     * Whether this version component is considered to supersede
     * other versions if the other versions length is shorter.
     *
     * - 1.2.0 > 1.2.0-BETA
     * - 1.2.0 < 1.2.0.1
     * - 1.2.0 = 1.2.0.0
     */
    fun compareWhenNotSpecified(): Int

    override fun hashCode(): Int
    override fun equals(other: Any?): Boolean

    class Numeric(val number: Int) : VersionPart {
        override fun canBeComparedTo(other: VersionPart): Boolean = other is Numeric
        override fun getFriendlyString(short: Boolean): String = number.toString()
        override fun compareWhenNotSpecified(): Int = this.number.compareTo(0)
        override fun asDataString(): String = number.toString()
        override fun hashCode(): Int = number.hashCode()
        override fun equals(other: Any?): Boolean = other is Numeric && this.number == other.number
        override fun compareTo(other: VersionPart): Int = when (other) {
            is Numeric -> this.number.compareTo(other.number)
            is Stage -> 1
            else -> 0
        }
    }

    class Unknown(val id: String) : VersionPart {
        override fun canBeComparedTo(other: VersionPart): Boolean = other is Unknown
        override fun asDataString(): String = id
        override fun getFriendlyString(short: Boolean): String = id
        override fun compareWhenNotSpecified(): Int = 1
        override fun hashCode(): Int = id.hashCode()
        override fun equals(other: Any?): Boolean = other is Unknown && this.id == other.id
        override fun compareTo(other: VersionPart): Int = when (other) {
            is Unknown -> this.id.compareTo(other.id)
            else -> 0
        }
    }

    enum class PreReleaseType(val unstable: Boolean, vararg aliases: String) : Comparable<PreReleaseType> {
        // Unstable/Development releases
        ALPHA(true, "a", "unstable"), BETA(true, "b", "dev", "prerelease", "snapshot"), RELEASE_CANDIDATE(true, "rc"),

        // Stable releases
        RELEASE(false, "distribution", "dist", "stable");

        val aliases: Set<String> = setOf(this.name.lowercase(Locale.ENGLISH)).plus(aliases.toSet())

        companion object {
            @JvmField val MAPPING: Map<String, PreReleaseType> =
                Enums.createMultiMapping(values(), { it.aliases }, hashMapOf())
        }
    }

    class Stage(val type: PreReleaseType) : VersionPart {
        override fun canBeComparedTo(other: VersionPart): Boolean = other is Stage
        override fun getFriendlyString(short: Boolean): String {
            return type.name.lowercase(Locale.ENGLISH).replace("_", "-")
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
        }

        override fun compareWhenNotSpecified(): Int = if (this.type.unstable) -1 else 1
        override fun hashCode(): Int = type.ordinal
        override fun equals(other: Any?): Boolean = other is Stage && this.type == other.type
        override fun asDataString(): String = when (this.type) {
            PreReleaseType.RELEASE -> ""
            else -> this.type.aliases.min()
        }

        override fun compareTo(other: VersionPart): Int = when (other) {
            is Stage -> this.type.compareTo(other.type)
            else -> 0
        }
    }
}

interface Version : VersionBase<Version> {
    override fun supersedes(other: Version): Boolean = compareTo(other) > 0
    override fun precedes(other: Version): Boolean = compareTo(other) < 0
    override fun canBeComparedTo(other: Version): Boolean
    override fun getFriendlyString(short: Boolean): String = asString(true, short)
    fun asString(prefix: Boolean, short: Boolean): String {
        val str = getParts().joinToString { it.getFriendlyString(short) }
        return if (prefix) "v$str" else str
    }

    override fun asDataString(): String = getParts().joinToString { it.asDataString() }
    fun getOriginalString(): String

    override fun hashCode(): Int
    override fun equals(other: Any?): Boolean
    fun getParts(): List<VersionPart>

    companion object {
        @JvmStatic fun parsePart(str: String): VersionPart {
            try {
                val num = Integer.parseInt(str)
                if (num >= 0) return VersionPart.Numeric(num)
            } catch (ignored: NumberFormatException) {
            }
            VersionPart.PreReleaseType.MAPPING[str.lowercase(Locale.ENGLISH)]?.let {
                return VersionPart.Stage(it)
            }
            return VersionPart.Unknown(str)
        }
    }
}

open class AbstractVersion(private val originalString: String, private val parts: List<VersionPart>) : Version {
    init {
        require(parts.isNotEmpty()) { "Version is empty" }
    }

    override fun getParts(): List<VersionPart> = this.parts
    override fun compareTo(other: Version): Int {
        val otherParts = other.getParts()
        val size = Math.min(parts.size, otherParts.size)
        for (i in 0..size) {
            val part = parts[i]
            val otherPart = otherParts[i]
            val compareTo = part.compareTo(otherPart)
            if (compareTo != 0) return compareTo
        }

        val nextAfterFinal = if (parts.size > size) {
            parts[size]
        } else {
            otherParts[size]
        }

        return nextAfterFinal.compareWhenNotSpecified()
    }

    override fun canBeComparedTo(other: Version): Boolean = true
    override fun getOriginalString(): String = originalString

    override fun equals(other: Any?): Boolean {
        if (other !is Version) return false;
        val parts = this.getParts()
        val otherParts = other.getParts()

        if (parts.size != otherParts.size) return false
        for (i in 0..parts.size) {
            val part = parts[i]
            val otherPart = otherParts[i]
            if (part != otherPart) return false
        }

        return true
    }

    override fun hashCode(): Int {
        var result = 0
        var first = true

        for (part in parts) {
            val hash = part.hashCode()
            if (first) {
                result = (hash xor (hash ushr 32))
                first = false
            } else {
                result = 31 * result + (hash xor (hash ushr 32))
            }
        }

        return result
    }
}