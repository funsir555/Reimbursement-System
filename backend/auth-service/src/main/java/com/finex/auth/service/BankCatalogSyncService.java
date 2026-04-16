package com.finex.auth.service;

import com.finex.auth.service.bankcatalog.BankCatalogProviderType;

public interface BankCatalogSyncService {

    BankCatalogProviderType getProviderType();

    default void sync() {
        throw new UnsupportedOperationException("Bank catalog sync is not implemented for this provider yet");
    }
}
