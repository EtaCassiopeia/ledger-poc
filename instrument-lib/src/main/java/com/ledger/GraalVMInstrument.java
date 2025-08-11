package com.ledger;

import org.graalvm.polyglot.HostAccess;

/**
 * GraalVM-specific instrument interface that extends the base Instrument interface
 * with GraalVM sandbox annotations.
 */
@HostAccess.Implementable
public interface GraalVMInstrument extends Instrument {
    // Inherits the process method from Instrument interface
}
