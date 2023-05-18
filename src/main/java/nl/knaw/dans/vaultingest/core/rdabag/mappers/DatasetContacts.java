package nl.knaw.dans.vaultingest.core.rdabag.mappers;

import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetContact;
import nl.knaw.dans.vaultingest.core.rdabag.mappers.vocabulary.DVCitation;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;

import java.util.Optional;

public class DatasetContacts {

    public static Optional<Statement> toDatasetContact(Resource resource, DatasetContact datasetContact) {
        if (datasetContact == null) {
            return Optional.empty();
        }

        var model = resource.getModel();
        var authorElement = model.createResource();

        authorElement.addProperty(DVCitation.datasetContactName, datasetContact.getName());
        authorElement.addProperty(DVCitation.datasetContactEmail, datasetContact.getEmail());

        if (datasetContact.getAffiliation() != null) {
            authorElement.addProperty(DVCitation.datasetContactAffiliation, datasetContact.getAffiliation());
        }

        return Optional.of(model.createStatement(
            resource,
            DVCitation.datasetContact,
            authorElement
        ));
    }
}