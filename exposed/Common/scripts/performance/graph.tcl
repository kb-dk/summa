#$Id:$
# logfiles: An array of logfiles to process
# graphfile The name of the bitmap that the graph should be rendered to, sans extension

# skipqueries: Skip the first x queries when calculating the average queries/second

proc makeGraph { logfiles graphfile xmin maxSearches ymin ymax xlogscale xminthumb xmaxthumb ymaxthumb plotthumb skipqueries smooth title } {
    puts "Extracting data..."
    set plotcommand "plot "
    set plotcommandnotitle "plot "
    set counter 0
    set firstSeconds 0
    foreach logfile $logfiles {
	puts -nonewline $logfile
	set fid [open $logfile r]
	set lines [split [read $fid] "\n"]
	close $fid
	
	set fid [open "tempdata_$logfile.tmp" w]
	puts $fid "Second\tSearchcount\tSpeed"
	set totalspeed "N/A"
	foreach line $lines {
	    if { [regexp {([0-9]+) sec\. ([0-9]+).+Hits\: ([0-9]+)\. Q/sec\: ([0-9]+\.[0-9]+) .([0-9]+\.[0-9]+)} $line result sec count hits speed totalspeed] } {
		#	puts "$sec $count $hits $speed"
		if { $count < $skipqueries } {
		    set firstSeconds $sec
		}
		puts $fid "$sec\t$count\t$speed"
		if { $count > $maxSearches } {
		    break
		}
	    } else {
		if { [regexp {Tested.*second\: ([0-9]+)\.[0-9]+} $line result totalspeed] } {
		    puts -nonewline " $logfile: $totalspeed queries/second"
		}
	    }
	}
	close $fid
	
	if { $skipqueries > 0 } {
	    puts -nonewline "\traw $totalspeed q/s"
	    set totalspeed [expr round(($count - $skipqueries) * 1.0 / ($sec - $firstSeconds) * 10) / 10.0]
	}
	puts "\t$totalspeed q/sec"

	incr counter
	set plotcommand "$plotcommand\"tempdata_$logfile.tmp\" using 2:3 title '$logfile ($totalspeed q/s)'"
	set plotcommandnotitle "$plotcommandnotitle\"tempdata_$logfile.tmp\" using 2:3"
	if { $smooth } {
	    set plotcommand "$plotcommand smooth bezier"
	    set plotcommandnotitle "$plotcommandnotitle smooth bezier"
	}
	if { $counter < [llength $logfiles] } {
	    set plotcommand "$plotcommand, \\\n"
	    set plotcommandnotitle "$plotcommandnotitle, \\\n"
	} else {
	    set plotcommand "$plotcommand;"
	    set plotcommandnotitle "$plotcommandnotitle;"
	}
    }
    
    
    puts "Generating gnuplot file for logfiles"
    
    set plotfile "tempgnuplot.pl"
    set fid [open $plotfile w]
    set width 750
    set height 550
    set width [expr 1.0 * $width / 640]
    set height [expr 1.0 * $height / 480]
    
    if { $xlogscale } {
	set mainxlog "set logscale x"
    } else {
	set mainxlog ""
    }

    puts $fid "set terminal png
set output \"$graphfile.png\"
set origin 0,0.02
set size $width,$height

set title \"$title\"
set data style lines
set xlabel \"Query \#\"
$mainxlog
set ylabel \"Searches/second\"
set xrange \[ $xmin : $maxSearches \]
set yrange \[ $ymin : $ymax \]

set multiplot
$plotcommand

set ylabel
set xlabel
set logscale x
set xrange \[ $xminthumb : $xmaxthumb \]
set yrange \[ 0 : $ymaxthumb \]
set size 0.5,0.4
set origin 0.09,0.69
set title ''
set nokey"
    if { $plotthumb } {
	puts $fid $plotcommandnotitle
    }

    close $fid

    puts "Executing tempgnuplot.pl to generate $graphfile"
    exec gnuplot "tempgnuplot.pl"
    
    puts "Cleaning up"
    set temps [glob "tempdata*.tmp"]
    foreach temp $temps {
	file delete $temp
    }
    file delete tempgnuplot.pl
    
    puts "Finished generating $graphfile"
}


