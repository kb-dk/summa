source "graph.tcl"

if { !([llength $argv] >= 2) } {
    puts stderr "Usage: graph logfiles graphname"
    exit
}

set logfiles [lrange $argv 0 end-1]
set graphfile [lindex $argv [expr [llength $argv] - 1]]
set maxSearches 115000
set ymax 300
set ymaxthumb 200
set plotthumb true

makeGraph $logfiles $graphfile $maxSearches $ymax $ymaxthumb $plotthumb
