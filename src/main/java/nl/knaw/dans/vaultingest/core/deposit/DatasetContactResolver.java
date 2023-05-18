package nl.knaw.dans.vaultingest.core.deposit;


import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetContact;

public interface DatasetContactResolver {

    DatasetContact resolve(String userId);

}
