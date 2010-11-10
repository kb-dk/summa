TODO
====
Error handling (ie. what happens if get_nat.sh full is run on  aday
when no full dump exists on the remote server?).

SETUP
=====
Edit get_nat.sh and set the BASEDIR path.

RUNNING IT
==========
Initially run:

get_nat.sh full

This will fetch the full dump of the day.

Then run:

get_nat.sh symlinks

This will remove old symlinks and create new ones pointing to the
latest full dump.

Afterwards run:

get_nat.sh diff

This will fetch all diffs that haven't already been fetched since the
date of the latest full dump.