package nl.knaw.dans.vaultingest.core.utilities;

import nl.knaw.dans.vaultingest.core.deposit.DatasetContactResolver;
import nl.knaw.dans.vaultingest.core.domain.metadata.DatasetContact;

public class EchoDatasetContactResolver implements DatasetContactResolver {
    @Override
    public DatasetContact resolve(String userId) {
        if (userId == null) {
            return null;
        }

        return DatasetContact.builder()
            .name(userId)
            .affiliation(userId + " university")
            .email(userId + "@test.com")
            .build();
    }
}
