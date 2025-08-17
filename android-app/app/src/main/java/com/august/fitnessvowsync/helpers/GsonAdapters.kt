package com.august.fitnessvowsync.helpers

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import java.math.BigInteger
import java.time.Duration
import java.time.Instant

class InstantTypeAdapter : TypeAdapter<Instant>() {
    override fun write(out: JsonWriter, value: Instant) {
        out.value(value.toString())
    }

    override fun read(reader: JsonReader): Instant {
        return Instant.parse(reader.nextString())
    }
}

class DurationTypeAdapter : TypeAdapter<Duration>() {
    override fun write(out: JsonWriter, value: Duration) {
        out.value(value.toString())
    }

    override fun read(reader: JsonReader): Duration {
        return Duration.parse(reader.nextLong().toString())
    }
}

class BigIntegerTypeAdapter : TypeAdapter<BigInteger>() {
    override fun write(out: JsonWriter, value: BigInteger) {
        // Write as a JSON string to avoid precision issues
        out.value(value.toString())
    }

    override fun read(reader: JsonReader): BigInteger {
        // Read the string value and convert to BigInteger
        return BigInteger(reader.nextString())
    }
}