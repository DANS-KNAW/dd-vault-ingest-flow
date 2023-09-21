Development
===========
This page contains information for developers about how to contribute to this project.

Set-up
------
This project can be used in combination with  [dans-dev-tools]{:target=_blank}. 
Before you can start it as a service some dependencies must first be started.
One dependency is [dd-validate-dans-bag]{:target=_blank}.
For the other dependency you have the choice between a local service [dd-vault-catalog]{:target=_blank} and a Virtual Machine (VM) `dev_transfer`.
The local variant requires a local database. 
For the VM you need access to the project [dd-dtap]{:target=_blank},

[dans-dev-tools]: https://github.com/DANS-KNAW/dans-dev-tools#readme
[dd-dtap]: https://github.com/DANS-KNAW/dd-dtap#readme
[dd-validate-dans-bag]: https://github.com/DANS-KNAW/dd-validate-dans-bag#readme
[dd-vault-catalog]: https://github.com/DANS-KNAW/dd-vault-catalog#readme

### Initialize development environment

This is only necessary once per project. If you execute this any existing configuration and data will be reset.

Open separate terminal tabs for `dd-vault-ingest-flow`, its dependency `dd-validate-dans-bag`
and the optional dependency (when not using the VM `dev_transfer`) `dd-vault-catalog`. In each tab run:

```commandline
start-env.sh
```

The service `dd-validate-dans-bag` needs different configurations for `dd-vault-ingest-flow` and other services. 
So you will have to update the generated `dd-validate-dans-bag/etc/config.yml`,
perhaps keep copies of the files or comment lines to switch between both situations.

* `dataverse: null`
* `vaultCatalog.baseUrl: https://dev.transfer.dans-data.nl/vault-catalog`

You will also have to adjust `dd-vault-ingest-flow/etc/config.yml`:

* vaultCatalog.url: https://dev.transfer.dans-data.nl

Both URLs in the above configuration examples assume you use the VM `dev_transfer`, 
if not, keep the URLs as generated from `src/test/resources/debug-etc`.

### Start services

To start the VM, run in the root of `dd-dtap`:

```commandline
start-preprovisioned-box.py -s dev_transfer
```

Without the VM you will need a local database for the service `dd-vault-catalog`, run in a separate terminal tab:

```commandline
start-hsqldb-server.sh
```

Pick the appropriate `vaultCatalog.url` in `dd-vault-ingest-flow/etc/config.yml`.
Open terminal tabs for the services `dd-vault-ingest-flow`, `dd-validate-dans-bag`, optionally (when not using the VM `dev_transfer`) `dd-vault-catalog` and run in each:

```commandline
start-service.sh
```

### Create a deposit

* Create a directory `/vagrant/shared/<UUID>`, this location makes it available on VMs at `/vagrant/shared`.
* Copy a valid bag into that directory, for example:

        dd-dans-sword2-examples/src/main/resources/example-bags/valid/default-open

  Note that `all_mappings` has an invalid prefix for `Has-Organizational-Identifier` in `bag-info.txt`.

  * Create a file in the same directory named `deposit.properties`, an example for the content:

        bag-store.bag-id = <UUID>
        dataverse.bag-id = urn:uuid:<UUID>
        dataverse.sword-token = sword:<UUID>
        creation.timestamp = 2023-08-16T17:40:41.390209+02:00
        deposit.origin = SWORD2
        depositor.userId = user001
        bag-store.bag-name = default-open

    Note that 
    * The `bag-name` should match the copied bag.
    * The `userId` should match a value configured as a `dataSupplier` in `dd-vault-ingest-flow/etc/config.yml`.
    * The `<UUID>` should match the directory name.
* To test updates you will need different UUIDs for each `example-bags/valid/*` and move them to the inbox.
  Note that the `revision02` and `revision03` bags lack an `Is-Version-Of: <UUID>` in `bag-info.txt`.
  Adding this property requires an update of the `tagmanifest-sha1.txt`.
  A validation error message will show the proper value, for example (with a locally running validator):

      cd dd-dans-sword2-examples
      ./run-validation.sh http://localhost:20330/validate /vagrant/shared/<UUID>/<bag-name>

## Start an ingest

To start an ingest locally, move (not copy, otherwise the processing might start before the copy completed)
a deposit into one of the inboxes configured in:

    dd-vault-ingest-flow/etc/config.yml

You can examine details of the result on `dev_transfer` in `/var/opt/dans.knaw.nl/tmp/ocfl-tar/inbox`
and the database: 

    sudo su postgres
    psql dd_vault_catalog
    select bag_id, data_supplier from ocfl_object_versions;
    \c dd_transfer_to_vault
    select bag_id, data_supplier from transfer_item;

Or examine the local transfer results in `dd-vault-ingest-flow/data/rda-bags`
and with `start-hsqldb-client.sh` executed in `dd-vault-catalog`.
The start dialog needs `database.url` specified in `dd-vault-catalog/etc/config.yml`.

### Test after deploy

The catalog results can be found on the VM `dev_transfer`,
the service under test (`dd-vault-ingest-flow`) should be deployed on `dev_vaas`, both VMs should be running.

Make the deposits available that were prepared in `/vagrant/shared`:

    cd /var/opt/dans.knaw.nl/tmp/auto-ingest # configured in /etc/opt/dans.knaw.nl/dd-vault-ingest-flow/config.ym
    cp -r `readlink -m /vagrant/shared/*/revision*/..` .. # or the actually wrapped bag(s)
    chmod -R 777 ../*7*

Start an ingest:

    mv -v `readlink -m ../*/revision01/..` inbox

