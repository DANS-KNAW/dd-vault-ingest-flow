dd-vault-ingest
====================

Service that processes deposits converting them to RDA compliant bags and sends them to the vault.

SYNOPSIS
--------

    dd-vault-ingest { server | check }

DESCRIPTION
-----------

This service is part of the DANS Vault Service. It is responsible for processing deposits, converting them to [RDA compliant bags]{:target=_blank}
and sending them to the DANS Data Vault. This service resembles [dd-ingest-flow]{:target=_blank} in that it processes deposits and that these deposits
eventually end up in the Vault. However, this service differs from dd-ingest-flow in that it does not create a Dataverse dataset version for each deposit,
relying on Dataverse to automatically export the dataset version to and RDA compliant bag. Instead, this service creates the RDA compliant bag directly from the
deposit. That said, the resulting bag is still designed to closely resemble the Dataverse bag.

### Ingest areas

An ingest area is a directory on local disk storage that is used by the service to receive deposits. It contains the following subdirectories:

* `inbox` - the directory under which all input deposits must be located
* `outbox` - a directory where the processed deposit are moved to (if successful to a subdirectory `processed`, otherwise to one of `rejected` or `failed`)

The service currently supports only one ingest area: `auto-ingest` - for continuous import of deposits offered through deposit service, such as
[dd-sword2]{:target=_blank}.

### Processing of a deposit

#### Order of deposit processing

A deposit directory represents one dataset version. The version history of a datasets is represented by a sequence of deposit directories. When enqueuing
deposits the program will first order them by the timestamp in the `Created` element in the contained bag's `bag-info.txt` file.

#### Processing steps

The processing of a deposit consists of the following steps:

##### Basic scenario

1. Check that the deposit is a valid [deposit directory]{:target=_blank}.
2. Check that the bag in the deposit is a valid v1 [DANS bag]{:target=_blank}.
3. Generate an NBN persistent identifier for the dataset and use that for the `dansNbn` field in the vault metadata.
4. Create a new, zipped RDA compliant bag from the deposit.
5. Register the bag in the dd-vault-catalog with minimal metadata: bag ID, NBN, and swordToken.
6. Move the deposit to the `outbox/processed` directory and change its state to `RECEIVED`.

###### Update scenario

2a Part of the validation will be to check that the deposit is an update to an existing dataset by checking that the `Is-Version-Of` field in the `bag-info.txt`
file of the deposit matches the `swordToken` of a dataset in the vault catalog.

3a Instead of generating a new NBN, the Vault Catalog will be queried for the NBN of the dataset that is being updated.

<!-- todo:  
- link to metadata mapping spreadsheet
- how to validate that a user account is authorized to update a dataset?
-->


ARGUMENTS
---------

        positional arguments:
        {server,check}         available commands
        
        named arguments:
        -h, --help             show this help message and exit
        -v, --version          show the application version and exit

INSTALLATION AND CONFIGURATION
------------------------------
Currently, this project is built as an RPM package for RHEL7/CentOS7 and later. The RPM will install the binaries to
`/opt/dans.knaw.nl/dd-vault-ingest` and the configuration files to `/etc/opt/dans.knaw.nl/dd-vault-ingest`.

For installation on systems that do no support RPM and/or systemd:

1. Build the tarball (see next section).
2. Extract it to some location on your system, for example `/opt/dans.knaw.nl/dd-vault-ingest`.
3. Start the service with the following command
   ```
   /opt/dans.knaw.nl/dd-vault-ingest/bin/dd-vault-ingest server /opt/dans.knaw.nl/dd-vault-ingest/cfg/config.yml 
   ```

BUILDING FROM SOURCE
--------------------
Prerequisites:

* Java 11 or higher
* Maven 3.3.3 or higher
* RPM

Steps:

    git clone https://github.com/DANS-KNAW/dd-vault-ingest.git
    cd dd-vault-ingest 
    mvn clean install

If the `rpm` executable is found at `/usr/local/bin/rpm`, the build profile that includes the RPM
packaging will be activated. If `rpm` is available, but at a different path, then activate it by using
Maven's `-P` switch: `mvn -Pprm install`.

Alternatively, to build the tarball execute:

    mvn clean install assembly:single

[RDA compliant bags]: https://www.rd-alliance.org/system/files/Research%20Data%20Repository%20Interoperability%20WG%20-%20Final%20Recommendations_reviewed_0.pdf

[dd-sword2]: https://dans-knaw.github.io/dd-sword2/

[dd-ingest-flow]: https://dans-knaw.github.io/dd-ingest-flow/

[deposit directory]: {{ deposit_directory }}

[DANS bag]: {{ dans_bagit_profile }}
