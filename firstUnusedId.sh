psql -U feideus -d sample_database2 -c "\i tryout.sql" | sed '3q;d'
