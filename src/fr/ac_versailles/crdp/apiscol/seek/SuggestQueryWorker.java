package fr.ac_versailles.crdp.apiscol.seek;

import java.util.UUID;

import javax.ws.rs.core.MediaType;

import org.w3c.dom.Document;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import fr.ac_versailles.crdp.apiscol.restClient.LanWebResource;

public class SuggestQueryWorker implements Runnable {

	private final String query;
	private final LanWebResource webServiceResource;
	private final SeekApi caller;
	private final UUID identifier;

	public SuggestQueryWorker(String query, LanWebResource contentWebServiceResource,
			SeekApi caller, UUID identifier) {
		this.query = query;
		this.webServiceResource = contentWebServiceResource;
		this.caller = caller;
		this.identifier = identifier;

	}

	@Override
	public void run() {
		ClientResponse contentWebServiceResponse = webServiceResource
				.path("suggestions")
				.queryParam("query", query)
				.accept(MediaType.APPLICATION_XML_TYPE)
				.get(ClientResponse.class);
		Document response = contentWebServiceResponse
				.getEntity(Document.class);
		caller.notifyRequestTermination(identifier, response);

	}
}
