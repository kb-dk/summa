/*
 * This batch job sets modificationTime to current time. Normally used to trigger an update of a specific target
 * by the indexer.
 */
commit=true;

if (state == null)
    state = 0;
state++;
if (last)
    out.print("Touched " + state + " records");
