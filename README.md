# strain-vt-annotation-pipeline

Propagates VT (Vertebrate Trait) annotations from QTLs to their associated strains.

## Overview

When a QTL has a VT annotation, each strain associated with that QTL should also carry that annotation.
This pipeline automates that propagation, keeping strain VT annotations in sync with QTL VT annotations.

## Logic

1. **Load QTL VT annotations** — retrieves all VT-aspect annotations on active QTLs
2. **Propagate to strains** — for each QTL annotation, looks up associated strains
   and creates a corresponding strain annotation with evidence code EXP
3. **Insert or match** — new strain annotations are inserted;
   existing matches have their last-modified timestamp updated
4. **Delete obsolete** — strain VT annotations created by this pipeline
   that were not seen in the current run are deleted

## Logging

- `status` — pipeline progress and summary counters
- `inserted` / `deleted` — audit logs for each annotation change

## Configuration

Configured in `properties/AppConfigure.xml`:
- `createdBy` — the pipeline user account ID used for annotation ownership

## Build and run

Requires Java 17. Built with Gradle:
```
./gradlew clean assembleDist
```
