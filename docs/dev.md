Development
===========
This page contains information for developers about how to contribute to this project.

Set-up
------
This project can be used in combination with  [dans-dev-tools]{:target=_blank}. Before you can start it as a service
some dependencies must first be started:

### Initialize development environment

This is only necessary once per project. If you execute this any existing configuration and data will be reset.

Open a separate terminal tab for `dd-vault-ingest-flow` and one for its dependency `dd-validate-dans-bag` and in each one run:

```commandline
start-env.sh
```

The service `dd-validate-dans-bag` needs different configurations for `dd-vault-ingest-flow` and other services. 
So you will have to update the generated `dd-validate-dans-bag/etc/config.yml`,
perhaps keep copies of the files or comment lines to switch between both situations.

* remove one level from `../../` from the schema locations.
* dataverse: null
* vaultCatalog.baseUrl: https://dev.transfer.dans-data.nl/vault-catalog

You will also have to adjust `dd-vault-ingest-flow/etc/config.yml`:

* vaultCatalog.url: https://dev.transfer.dans-data.nl

Both URLs in the above configuration examples assume you use the virtual machine (VM) `dev_transfer` and not additional local services and databases.
Keep the URLs as generated when you don't use a VM.

### Start services

When you are granted access to the project `dd-dtap` you can start the VM configured above.

```commandline
start-preprovisioned-box.py -s dev_transfer
```

Without the VM you will need a local database for the service `dd-vault-catalog`, run in a separate terminal:

```commandline
start-hsqldb-server.sh
```

Open new terminals for the services `dd-vault-ingest-flow`, `dd-validate-dans-bag`, optionally (without a VM) `dd-vault-catalog` and run:

```commandline
start-service.sh
```

### Create a deposit

* Create a directory with a `<UUID>` as name, for example `b8f7f836-013a-4e32-9070-2efcfab3316b`.
* Copy a valid bag into that directory, for example:

      dd-dans-sword2-examples/src/main/resources/example-bags/valid/default-open

  Note that `all_mappings` has an invalid prefix for `Has-Organizational-Identifier` in `bag-info.txt`,
  other bags should be valid.

* Create a file in the same directory named `deposit.properties`, an example for the content

      bag-store.bag-id = b8f7f836-013a-4e32-9070-2efcfab3316b
      dataverse.bag-id = urn:uuid:b8f7f836-013a-4e32-9070-2efcfab3316b
      creation.timestamp = 2023-08-16T17:40:41.390209+02:00
      deposit.origin = SWORD2
      depositor.userId = user001
      bag-store.bag-name = default-open
      dataverse.sword-token = sword:b8f7f836-013a-4e32-9070-2efcfab3316b

  Note that the `bag-name` should match the copied bag and the `userId` should match a value configured as a `dataSupplier` in `dd-vault-ingest-flow/etc/config.yml`.

## Start an ingest

To start an ingest, move (not copy, otherwise the processing might start before the copy the completed)
a deposit into one of the inboxes configured in:

    dd-vault-ingest-flow/etc/config.yml

You can examine details of the result on the VM in `/var/opt/dans.knaw.nl/tmp/ocfl-tar/inbox`
and the database: 

    sudo su postgres
    psql dd_vault_catalog

[dans-dev-tools]: https://github.com/DANS-KNAW/dans-dev-tools#dans-dev-tools
