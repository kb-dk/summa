#$Id:$
source "graph.tcl"

if { !([llength $argv] >= 2) } {
    puts stderr "Usage: graph \[-t\] \[-xmin int\] \[-xmax int\] \[-ymin int\] \[-ymax int\]"
    puts stderr "             \[xlogscale\] \[smooth boolean\] \[title string\] \[-skipqueries int\] logfiles graphname"
    puts stderr " "
    puts stderr "-t:        Print logarithmic thumb"
    puts stderr "-xmin int: Minimum for the x-axis"
    puts stderr "-xmax int: Maximum for the x-axis"
    puts stderr "-ymin int: Minimum for the y-axis"
    puts stderr "-ymax int: Maximum for the y-axis"
    puts stderr "-xlogscale: Use logarithmic scaling for the x-axis"
    puts stderr "-smooth boolean: Use smoothing"
    puts stderr "-title string: The title for the graph"
    puts stderr "-skipqueries int: Skip this number of queries before calculating average queries/second"
    exit
}

set logstart 0
set plotthumb false
set xmin 1000
set xmax 340000
set ymin 0
set xlogscale false
set smooth true
set ymax 400
set title "Search performance on logged queries"
set skipqueries 0

if { [lindex $argv $logstart] == "-t" } {
    set plotthumb true
    incr logstart
}
if { [lindex $argv $logstart] == "-xmin" } {
    incr logstart
    set xmin [lindex $argv $logstart]
    incr logstart
}
if { [lindex $argv $logstart] == "-xmax" } {
    incr logstart
    set xmax [lindex $argv $logstart]
    incr logstart
}
if { [lindex $argv $logstart] == "-ymin" } {
    incr logstart
    set ymin [lindex $argv $logstart]
    incr logstart
}
if { [lindex $argv $logstart] == "-ymax" } {
    incr logstart
    set ymax [lindex $argv $logstart]
    incr logstart
}
if { [lindex $argv $logstart] == "-xlogscale" } {
    incr logstart
    set xlogscale true
}
if { [lindex $argv $logstart] == "-smooth" } {
    incr logstart
    set smooth [lindex $argv $logstart]
    incr logstart
}
if { [lindex $argv $logstart] == "-title" } {
    incr logstart
    set title [lindex $argv $logstart]
    incr logstart
}
if { [lindex $argv $logstart] == "-skipqueries" } {
    incr logstart
    set skipqueries [lindex $argv $logstart]
    incr logstart
}
set logfiles [lrange $argv $logstart end-1]
set graphfile [lindex $argv [expr [llength $argv] - 1]]

if { [file exists $graphfile] } {
    puts stderr "$graphfile exists as a file. This probably means that you forgot to provide a graphname."
    exit
}

set xminthumb 10
#set xmaxthumb 1000
set xmaxthumb $xmax
set ymaxthumb 300

makeGraph $logfiles $graphfile $xmin $xmax $ymin $ymax $xlogscale $xminthumb $xmaxthumb $ymaxthumb $plotthumb $skipqueries $smooth $title
