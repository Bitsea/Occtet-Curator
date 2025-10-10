package eu.occtet.bocfrontend.service;

import eu.occtet.boc.model.MicroserviceDescriptor;

public interface IOnMicroserviceDescriptorReceived {
    void onMicroserviceDescriptorReceived(MicroserviceDescriptor descriptor);
}
