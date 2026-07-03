# Data Sources

## BKK GTFS demo data

This project includes a small curated GTFS demo archive:

```text
ingestor/src/main/resources/gtfs/budapest-mini.zip
```

It is derived from BKK Open Data GTFS data and is intentionally small so the MVP can be cloned, tested, and demonstrated quickly.

Attribution required by the source dataset license:

> Data source: BKK Zrt., CC BY 4.0

Source portal:

- https://opendata.bkk.hu/

License:

- Creative Commons Attribution 4.0 International (CC BY 4.0)
- https://creativecommons.org/licenses/by/4.0/deed.en

## Full GTFS archives

Full GTFS archives are not stored in this repository because they are large and slow down clone/test/demo workflows.

For full-data experiments, download the current dataset from BKK Open Data and keep it outside git, or use a local ignored file under `ingestor/src/main/resources/gtfs/`.
