package com.ledger;

import com.ledger.avro.JavaAvroUtils;
import com.ledger.avro.Message;
import com.ledger.avro.Result;

/**
 * Base class for instruments: keeps the host boundary as byte[] while exposing
 * a simple domain-level API for implementors.
 *
 * Implementors only override process(Message) and return Result.
 */
public abstract class BaseInstrument implements Instrument {
    /**
     * Domain-level business logic to be implemented by instrument authors.
     */
    protected abstract Result process(Message message);

    /**
     * Host boundary: invoked by the application. Handles Avro (de)serialization.
     */
    public final byte[] process(byte[] avroInput) {
        try {
            Message message = JavaAvroUtils.deserializeMessage(avroInput);
            Result result = process(message);
            return JavaAvroUtils.serializeResult(result);
        } catch (Exception e) {
            try {
                var errorAction = JavaAvroUtils.createAction("ERROR", "Instrument failed: " + e.getMessage());
                var errorResult = JavaAvroUtils.createResult(errorAction);
                return JavaAvroUtils.serializeResult(errorResult);
            } catch (Exception secondary) {
                throw new RuntimeException("Critical error in instrument", e);
            }
        }
    }
}
