package kb.dto;

import java.io.IOException;
import java.time.ZonedDateTime;


import org.eclipse.rdf4j.model.IRI;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

		public SodaliteAbstractModel(IRI iri) {
			super(iri);
		}
		
		public SodaliteAbstractModel(IRI iri, String version) {
			super(iri);
			this.version = version;
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
}
