/*
 * This batch job collects all record ids and returns them in a
 * newline-separated list.
 * In order to have 'wc -l <output> == <totalCount>' we don't add a trailing
 * newline - hence the conditional on 'last'.
 */
if (last)
    out.print(record.getId());
else
    out.println(record.getId());
