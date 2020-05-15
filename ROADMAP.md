# Blueprint

## Mindmap

* ~~Rename to `dapla-blueprint`~~
* Create graph
  * Operate file system folder
    * Can be a commit
    * Can be triggered by hooks
      * JGIT
  * Traverse all file
    * Filter on extension *.pynb
  * Parsing
    * Input/Output declarations
      * Use metadata in ipynb?
      * Jupyterlab widget?
    * Use file path
  * Model
    * Started. See https://github.com/statisticsnorway/blueprint
* Execution & Integration
  * Generate execution list
    * Stop on edges
    * Correct ordering
  * Generate Airflow DAG? 
  * External API
  * Integration with scheduler
    * A bit early?
* Infra
  * Neo4j
    * **Bolt in service mesh?**

## TODOs

1. Create application for parsing
   1. ipynb parser? 
   2. Input/Output metadata format in notebooks
   3. Output builders

```bash
app parse [--commit fe6d8sd] -output text/blueprint/cypher ./folder
```

2. Create service
   1. health endpoint
   2. metrics endpoint
3. Connect to github hooks
4. Parse on new commits

