package eu.occtet.bocfrontend.service;

import eu.occtet.boc.model.StatusDescriptor;

public interface IOnStatusDescriptorReceived {
    void onStatusDescriptorReceived(StatusDescriptor descriptor);
}
