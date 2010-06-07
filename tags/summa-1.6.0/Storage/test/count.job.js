/*
 * This batch job counts and returns the number of records it receives
 */
if (state == null)
    state = 0;
state++;
if (last)
    out.print(state);