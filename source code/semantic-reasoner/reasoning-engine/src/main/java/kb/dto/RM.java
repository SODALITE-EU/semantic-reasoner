package kb.dto;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.eclipse.rdf4j.model.IRI;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import kb.KBApi;
import kb.utils.MyUtils;

public class RM extends Resource {
	private static final Logger LOG = LoggerFactory.getLogger(RM.class.getName());

	IRI user;
	ZonedDateTime createdAt;
	String namespace;

	Set<NodeFull> types;
	
	boolean forRefactorer;
	
	public RM(IRI iri) {
		super(iri);
		types = new HashSet<NodeFull>();
	}

	public void build(KBApi api) throws IOException {
		for (NodeFull nodeFull : types) {
			nodeFull.build(api);
		}
	}

	public IRI getUser() {
		return user;
	}

	public void setUser(IRI user) {
		this.user = user;
	}

	public ZonedDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(ZonedDateTime createdAt) {
		this.createdAt = createdAt;
	}
	
	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
	
	public Set<NodeFull> getTypes() {
		return this.types;
	}

	public void setTemplates(Set<NodeFull> types) {
		this.types = types;
	}

	public void addType(NodeFull n) {
		types.add(n);
	}

	@Override
	public JsonElement serialise() throws IOException {
		JsonObject aadm = new JsonObject();

		JsonObject data = new JsonObject();
		data.addProperty("id", this.uri);
		data.addProperty("namespace", namespace);
		data.addProperty("type", "ResourceModel");
		data.addProperty("createdBy", user.toString());
		data.addProperty("createdAt", createdAt.toString());
		

		JsonArray participants = new JsonArray();
		for (NodeFull nodeFull : types) {
			participants.add(nodeFull.uri);
		}
		data.add("participants", participants);
		aadm.add(uri, data);

		// recursion

		LinkedList<NodeFull> list = new LinkedList<NodeFull>(types);
		HashSet<String> bag = new HashSet<String>();

		NodeFull nodeFull;
		while (!list.isEmpty()) {
			nodeFull = list.pop();
			if (nodeFull == null || bag.contains(nodeFull.uri))
				continue;
			bag.add(nodeFull.uri);
			JsonElement serialise = nodeFull.serialise();
			aadm.add(nodeFull.uri, serialise);

			LOG.info("Relevant Uris: {}", nodeFull.relevantUris);
			LOG.info("list: {}", list.size());
			LOG.info("bag: {}", bag.size());
		}

		LOG.info("Bag: {}", MyUtils.getGson(true).toJson(bag));

		return aadm;
	}

	@Override
	public JsonElement serialiseCompact() {
		return null;
	}

}
