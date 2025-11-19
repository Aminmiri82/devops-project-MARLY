package org.marly.mavigo.service;

import org.marly.mavigo.client.prim.PrimApiClient;
import org.marly.mavigo.client.prim.PrimDisruption;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PerturbationService {

    private final PrimApiClient primApiClient;

    public PerturbationService(PrimApiClient primApiClient) {
        this.primApiClient = primApiClient;
    }

    public List<PrimDisruption> getPerturbations() {
        return primApiClient.getDisruptions();
    }
}
