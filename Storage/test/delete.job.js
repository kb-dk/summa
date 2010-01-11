/*
 * This batch job marks all records as deleted and returns a |-separated
 * list of the record ids of the affected records
 */
commit=true;
record.setDeleted(true);
out.print(record.getId());
out.print("|");