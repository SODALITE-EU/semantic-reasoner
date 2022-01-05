package kb.dto;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.eclipse.rdf4j.model.IRI;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import kb.KBApi;
import kb.utils.MyUtils;

public class SodaliteAbstractModel extends Resource {

		IRI user;
		ZonedDateTime createdAt;
		String version;
		String dsl;
		String name;
		boolean isAADM;
		String namespace;
		String description;
		
		//needed by KB Browser view in IDE for rendering ansible DSL
		Set<NodeFull> types;

		public SodaliteAbstractModel(IRI iri) {
			super(iri);
			types = new HashSet<NodeFull>();
		}
		
		public SodaliteAbstractModel(IRI iri, String version) {
			super(iri);
			this.version = version;
		}
		
		public void build(KBApi api) throws IOException {
			if (types != null) {
				for (NodeFull nodeFull : types) {
					nodeFull.buildForInterfaces(api);
				}
			}
		}

		@Override
		public JsonElement serialise() throws IOException {
			JsonObject model = new JsonObject();
			model.addProperty("uri", uri);
			model.addProperty("createdBy", user.toString());
			model.addProperty("createdAt", createdAt.toString());
			model.addProperty("dsl", dsl);
			model.addProperty("name", name);
			if (version != null)
				model.addProperty("version", version);
			if (description != null)
				model.addProperty("description", description);
			
			//The interface information is needed for the types by the IDE KB Browser view related with ansible
			if (types != null) {
				LinkedList<NodeFull> list = new LinkedList<NodeFull>(types);
				JsonObject nodeTypes = new JsonObject();
			
				NodeFull nodeFull;
				while (!list.isEmpty()) {
					nodeFull = list.pop();
					JsonElement serialise = nodeFull.serialiseForInterfaces();
					if (serialise != null)
						nodeTypes.add(nodeFull.uri, serialise);
				}
				if(!types.isEmpty())
					model.add("nodeTypes", nodeTypes);
			}
			
			return model;
		}

		@Override
		public JsonElement serialiseCompact() {
			// TODO Auto-generated method stub
			return null;
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

		public String getVersion() {
			return version;
		}

		public void setVersion(String version) {
			this.version = version;
		}
		
		public String getDsl() {
			return name;
		}

		public void setDsl(String dsl) {
			this.dsl = dsl;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public boolean getIsAADM() {
			return isAADM;
		}

		public void setIsAADM(boolean isAADM) {
			this.isAADM = isAADM;
		}
		
		public String getNamespace() {
			return namespace;
		}

		public void setNamespace(String namespace) {
			this.namespace = namespace;
		}
		
		public String getDescription() {
			return namespace;
		}

		public void setDescription(String description) {
			this.description = description;
		}
		
		public void addType(NodeFull n) {
			types.add(n);
		}
		
		public Set<NodeFull> getTypes() {
			return this.types;
		}

		public void setTypes(Set<NodeFull> types) {
			this.types = types;
		}
}
