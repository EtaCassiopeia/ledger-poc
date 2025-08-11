package com.ledger;

/**
 * Interface for sandboxed instruments. Implementations must be pure functions,
 * taking binary Avro input and returning binary Avro output.
 * 
 * This interface is shared between the main application and instrument JARs,
 * ensuring type safety and compatibility.
 * 
 * The @HostAccess.Implementable annotation is added in the instrument-lib
 * where GraalVM dependencies are available.
 */
public interface Instrument {
    /**
     * Processes the input Avro binary data and returns result Avro binary data.
     * 
     * @param avroInput Binary Avro data representing a Message
     * @return Binary Avro data representing a Result
     * @throws RuntimeException if processing fails
     */
    byte[] process(byte[] avroInput);
}
