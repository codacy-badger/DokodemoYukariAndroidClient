package com.github.windsekirun.yukarisynthesizer.core.item

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import org.redundent.kotlin.xml.xml
import java.io.Serializable

@Entity
class Phonome: Serializable {

    @Id
    var id: Long = 0
    var origin: String = ""
    var phoneme: String = ""

    constructor(origin: String) {
        this.origin = origin
    }

    constructor(origin: String, phoneme: String) {
        this.origin = origin
        this.phoneme = phoneme
    }

    override fun toString(): String {
        return this.build()
    }

    companion object {
        @JvmStatic
        fun Phonome.build(): String {
            if (this.phoneme.isEmpty()) return this.origin
            return xml("phoneme") {
                attribute("ph", this@build.phoneme)
                -this@build.origin
            }.toString()
        }
    }
}