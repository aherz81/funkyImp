#!/usr/bin/ruby

require 'find'
require 'fileutils'

tool = ARGV[0]
if !tool then
  fail "tool not specified!"
end
if !File.exists?(tool) then
  fail "'"+ARGV[0]+"' not found!"
end

#puts "TOOL: #{tool}"

if ENV['FUNKY_LIB_PATH'] == nil then
	fail "environment variable FUNKY_LIB_PATH not set"
end

#make sure
#ENV['LD_LIBRARY_PATH'] = ENV['LD_LIBRARY_PATH']+":"+ENV['FUNKY_LIB_PATH']+"/lib"

$stderr.puts "FUNKY_LIB_PATH=#{ENV['FUNKY_LIB_PATH']}"

#backup = File.join(Dir.getwd,"tool.script_backup.json")
#json   = File.join(Dir.getwd, "tool.json")
#FileUtils.mv(json, backup) if File.exists?(json)

testresults = File.expand_path("tests/suite_result")
testfiles   = File.expand_path("tests/regression")

class Project
  attr_reader :name, :group, :path, :params, :warnings, :errc, :warnc, :noerr, :nowarn, :custom, :skip, :crash, :gid
  attr_writer :size, :errc, :warnc, :skip, :crash
  def initialize(id, gid, name, size, group, path, params, warnings,noerr,nowarn,errc,warnc,custom, skip)
    @id       = id
	@gid	  = gid
    @name     = name
    @size     = size
    @group    = group
    @path     = path
    @params   = params
    @warnings = warnings
	@crash = false
    @noerr = noerr
    @nowarn = nowarn
    @errc = errc
    @warnc = warnc
    @custom = custom
    @skip = skip
  end
  def to_html
    orgfile = group + "__" + name + ".c.html"
    cilfile = group + "__" + name + ".cil.txt"
    "<td><a href=\"file://#{path}\">#{@id}</a></td>\n" +
    "<td><a href=\"#{orgfile}\">#{@name}</a></td>\n" +
    "<td><a href=\"#{cilfile}\">#{@custom!=""}</a></td>\n"
  end
  def to_s
    "#{@name} (#{@url})"
  end
end

#Command line parameters
#Either only run a single test, or
#"future" will also run tests we normally skip
only = ARGV[1] unless ARGV[1].nil?
if only == "future" then
  future = true
  only = nil
elsif only == "group" then
  future = true
  thegroup = ARGV[2]
  only = nil
else
  future = false
end
# analyses = ["mutex", "base", "cpa", "intcpa"]
# analyses = ["mutex"]

tracing = false

#processing the file information
projects = []
regs = Dir.open(testfiles)
regs.sort.each do |d|
  next if File.basename(d)[0] == ?.
  gid = d[0..1]
  groupname = d[3..-1]
  next unless thegroup.nil? or groupname == thegroup
  grouppath = File.expand_path(d, testfiles)
  group = Dir.open(grouppath)
  group.sort.each do |f|
    next if File.basename(f)[0] == ?.
    next unless f =~ /.*\.funky$/
    if f[0..1].to_i != 0
      id = gid + "/" + f[0..1]
      testname = f[3..-1]
    else
      id = gid
      testname = f[0..-1]
    end
    next unless only.nil? or testname == only
    path = File.expand_path(f, grouppath)
    lines = IO.readlines(path)
    size = 0
    debug = true

    next if not future and only.nil? and lines[0] =~ /@SKIP/
    debug = false unless lines[0] =~ /@DEBUG/
    lines[0] =~ /@PARAM: (.*)$/
    if $1 then params = $1 else params = "" end

    nowarn = false
    noerr = false
    custom = ""

    req_count = 0

    hash = Hash.new
    i = 0
    lines.each do |obj|
      i = i + 1
      if obj =~ /@TEST (.*)/ then
        case $1
          when /noerror/
			noerr = true
          when /nowarning/
			nowarn = true
          else "other"
        end
      end
      if obj =~ /@CUSTOM (.*)/ then
        custom=$1
      end
      if obj =~ /#line ([0-9]+).*$/ then
        i = $1.to_i - 1
      end
       next if obj =~ /^\/\//
      if obj =~ /@NOWARNING\((.*)\)/ then
        hash[i] = "nowarning(#{$1})"
        req_count+=1
      elsif obj =~ /@NOERROR\((.*)\)/ then
        hash[i] = "noerror(#{$1})"
        req_count+=1
      elsif obj =~ /@ERROR\((.*)\)/ then
        hash[i] = "error(#{$1})"
        req_count+=1
        #puts "ERROR(#{$1})"
      elsif obj =~ /@WARNING\((.*)\)/ then
        hash[i] = "warning(#{$1})"
        req_count+=1
        #puts "warning(#{$1})"
      end
    end
    params << " --debug" if debug
    skip = req_count == 0 && !noerr && !nowarn && custom==""
    p = Project.new(id,gid,testname,size,groupname,path,params,hash,noerr,nowarn,0,0,custom,skip)
    projects << p
  end
end

#analysing the files
startdir = Dir.pwd
projects.each do |p|
  Dir.chdir(startdir)
  filepath = p.path
  dirname = File.dirname(filepath)
  filename = File.basename(filepath)
  Dir.chdir(dirname)
  $stderr.puts "testing: #{p.params} #{p.name}"
  warnfile = File.join(testresults, p.group + "__" + p.name + ".warn.txt")
  statsfile = File.join(testresults, p.group + "__" + p.name + ".stats.txt")
  orgfile = File.join(testresults, p.group + "__" + p.name + ".c.html")
  File.open(File.join(testresults, p.group + "__" + p.name + ".cil.txt"), "w") do |f|
    f.puts "CWD : #{Dir.getwd}"
    f.puts "tool : #{tool} #{filename} #{p.params}" # concrete invocation
    if (p.custom!="") then
      custom=p.custom
      custom=custom.gsub(/@error/,File.join(testresults, warnfile))
      custom=custom.gsub(/@output/,File.join(testresults, statsfile))
      f.puts "custom script : #{custom}"
    end
  end
  `code2html -l java -n #{filename} &> #{orgfile}`
  #`highlight --syntax c -l -i #{filename} -o #{orgfile}`
  #puts "#{tool} #{filename} #{p.params} 2>#{warnfile} 1>#{statsfile}"
  #puts "test:#{filename}"
  `#{tool} #{filename} #{p.params} 2>#{warnfile} 1>#{statsfile}` #get ret code??
  if $?.exitstatus > 1 then
	p.skip=false
	p.crash=true
	puts "compiler crash : #{filename}"
	`echo "#{filename}(0): >ERROR<compiler.abnormal> : abnormal compiler exit" >> #{warnfile}`
  end
end
#FileUtils.mv(backup,json) if File.exists?(backup)
error_count = 0

#Outputting
header = <<END
<head>
  <title>Tests (#{`uname -n`.chomp} #{`date`})</title>
<style type="text/css" media="screen">
/* <![CDATA[ */
@import url(../html/collapsible-tables.css);
/* ]]> */
</style>
<!--[if lte IE 7]>
  <link rel="stylesheet" type="text/css" href="../html/ie-7-hacks.css" media="screen" />
<![endif]-->
<script src="../html/collapsible-tables.js" type="text/javascript">
</script>
</head>
END
File.open(File.join(testresults, "index.html"), "w") do |f|
  f.puts "<html>"
  f.puts header
  f.puts "<body>"
  f.puts "(#{`uname -n`.chomp} #{`date`})"
  f.puts "<table class=\"outertable\">"
  f.puts "<tbody class=\"outerbody\">"
  gname = ""
  gid = ""
  headings = ["ID", "Name", "Custom", "Result", "Time", "Problems"]

  p_errors = 0
  is_first = true

  projects.each do |p|

	# close old table
    if p.group != gname then
		if is_first then
			is_first = false
		else
			f.puts "</tbody>"
			if p_errors == 0 then
				f.puts "<thead><tr><td colspan=\"6\"><a herf=\"#\" onclick=\"toggle(this); return false;\"><img src=\"../html/down-arrow.gif\" alt=\"X\"/><span style=\"padding-left:20px;color:green\">#{gid}-#{gname}</span></a></td></tr></thead>"
			else
				f.puts "<thead><tr><td colspan=\"6\"><a herf=\"#\" onclick=\"toggle(this); return false;\"><img src=\"../html/down-arrow.gif\" alt=\"X\"/><span style=\"padding-left:20px;color:red\">#{gid}-#{gname} : ERRORS (#{p_errors})</span></a></td></tr></thead>"
			end
			f.puts "</table>"
			f.puts "</td></tr>"
		end
    end

    is_ok = true

    if p.group != gname then
	  f.puts "<tr class=\"outer_tr\"><td class=\"outer_td\">"
	  f.puts "<table>"
	  f.puts "<tbody>"
    end


    if p.group != gname then
		gname = p.group
		gid = p.gid
		f.puts "<tr>"
		headings.each {|h| f.puts "<th>#{h}</th>"}
		f.puts "</tr>"

		p_errors = 0
    end

    f.puts "<tr>"

    f.puts p.to_html

    warnfile = p.group + "__" + p.name + ".warn.txt"
    customfile = p.group + "__" + p.name + ".custom.txt"
    warnings = Hash.new
    warnings[-1] = "term"
    lines = IO.readlines(File.join(testresults, warnfile))
    lines.each do |l|
      if l =~ /does not reach the end/ then warnings[-1] = "noterm" end
#      next unless l =~ /(.*)\(.*\:(.*)\)/ # write in some thread with lockset: {B_mutex} (32-allfuns.c:16)
      next unless l =~ /(.*)\((.*)\)\: >(ERROR|WARNING)<(.*)> .*/ # 01-BracesBug.funky(3) >ERROR<compiler.err.premature.eof>  reached end of file while parsing :	class Bug {   // ERROR(compiler.err.premature.eof)
      file,i,type,msg = $1,$2.to_i,$3,$4
#      puts "FOUND : '#{file}' '#{i}' '#{type}' '#{msg}'"
      if type =~/ERROR/ then
        p.errc+=1
      end
      if type =~/WARNING/ then
        p.warnc+=1
      end
      ranking = ["other", "warn", "race", "norace", "assert", "fail", "unknown", "term", "noterm"]
      thiswarn =  case type
                    when /ERROR/
						"error(#{msg})"
                    when /WARNING/
						"warning(#{msg})"
                    else "other"
                  end
      oldwarn = warnings[i]
      #if oldwarn.nil? then
        #puts "warnings[#{i}]=#{thiswarn}"
        warnings[i] = thiswarn
      #else
      #  warnings[i] = ranking[[ranking.index(thiswarn), ranking.index(oldwarn)].max]
      #end
    end
    correct = 0
    ferr = nil
    statsfile = p.group + "__" + p.name + ".stats.txt"

    if(p.custom=="") then
      p.warnings.each_pair do |idx, type|
        case type
        when /nowarning\((.*)\)/
          req = $1
          if warnings[idx].nil? || warnings[idx]!~/warning/ then correct += 1
          else
            warnings[idx]=~/.*\((.*)\)/
            found=$1
            puts "req : #{req}, found : #{found}"
            if req!="*" && req!=found then correct += 1
            else
              ferr = idx if ferr.nil? or idx < ferr
            end
          end
        when /noerror\((.*)\)/
          req = $1
          if warnings[idx].nil? || warnings[idx]!~/error/ then correct += 1
          else
            puts "type : #{warnings[idx]}"
            warnings[idx]=~/.*\((.*)\)/
            found=$1
            puts "req : #{req}, found : #{found}"
            if req!="*" && req!=found then correct += 1
            else
              ferr = idx if ferr.nil? or idx < ferr
            end
          end
        else #error(xxx) or warning(xxx)
          type=~/.*\((.*)\)/
          if $1=="*" then
            puts "'*' type only accepted for @NOERROR() and @NOWARNING(), @ERROR() and @WARNING() must use specific type."
            error_count+=1
          end
          if warnings[idx] == type then
            correct += 1
          else
            puts "Expected #{type}, but registered #{warnings[idx]} on #{p.name}:#{idx}"
            ferr = idx if ferr.nil? or idx < ferr
          end
        end
      end
    else
      #File.join(testresults, warnfile)
      custom=p.custom
      custom=custom.gsub(/@error/,File.join(testresults, warnfile))
      custom=custom.gsub(/@output/,File.join(testresults, statsfile))

      Dir.chdir(startdir)
      filepath = p.path
      dirname = File.dirname(filepath)
      filename = File.basename(filepath)
      Dir.chdir(dirname)
      #puts "CWD : #{Dir.getwd}"
      $stderr.puts "custom: '#{p.name} : #{custom}'"
      `#{custom} > #{File.join(testresults, customfile)}`
      correct = $?.exitstatus.to_i
    end
    if (p.errc>0 && p.noerr)||(p.warnc>0 && p.nowarn) then
      error_count+=1
      f.puts "<td><a href=\"#{warnfile}\">"
      if (p.errc>0 && p.noerr) then
        f.puts "#{p.errc}e "
      end
      if (p.warnc>0 && p.nowarn) then
        f.puts "#{p.warnc}w"
      end
      f.puts "</a></td>"
    else
      if (p.custom=="") then
        f.puts "<td><a href=\"#{warnfile}\">#{correct} of #{p.warnings.size}</a></td>"
      else
        f.puts "<td><a href=\"#{warnfile}\">#{correct}</a></td>"
      end
    end


    lines = IO.readlines(File.join(testresults, statsfile))
    res = lines.grep(/total \s*(.*)\].*$/) { |x| $1 }
    errors = lines.grep(/Error:/)
    if res == [] or not errors == [] then
      #is_ok = false
      f.puts "<td><a href=\"#{statsfile}\">messages</a></td>"
    else
      f.puts "<td><a href=\"#{statsfile}\">#{res.to_s}</a></td>"
    end

    if tracing then
      confile = p.group + "__" + p.name + ".con.txt"
      lines = IO.readlines(File.join(testresults, confile))
      cons = lines.grep(/con/).size
      f.puts "<td><a href=\"#{confile}\">#{cons} nodes</a></td>"
      solfile = p.group + "__" + p.name + ".sol.txt"
      lines = IO.readlines(File.join(testresults, solfile))
      sols = lines.grep(/sol: Entered/).size
      f.puts "<td><a href=\"#{solfile}\">#{sols} nodes</a></td>"
    end

    if p.skip then
        f.puts "<td style =\"color: yellow\">SKIP</td>"
        puts "No constraints defined for #{p.name}, skipped!"
        error_count+=1
    else if correct == p.warnings.size && is_ok && ! p.crash then
      if ((p.errc>0 && p.noerr)||(p.warnc>0 && p.nowarn)) then
        f.puts "<td style =\"color: red\">ERR/WARN</td>"
		p_errors+=1
      else
        f.puts "<td style =\"color: green\">NONE</td>"
      end
    else
      puts "#{p.name} : FAIL"
      error_count += 1
      if ferr.nil? then
        #f.puts "<td style =\"color: red\">FAIL</td>"
		p_errors+=1
        f.puts "<td><a href=\"#{File.join(testresults, customfile)}\" style =\"color: red\">FAILED</a></td>"
      else
		p_errors+=1
        whataglorifiedmess =p.group + "__" +  p.name + ".c.html"
        f.puts "<td><a href=\"#{whataglorifiedmess}#line#{ferr}\" style =\"color: red\">LINE #{ferr}</a></td>"
      end
    end
    end
    f.puts "</tr>"

  end

	f.puts "</tbody>"
	if p_errors == 0 then
		f.puts "<thead><tr><td colspan=\"6\"><a herf=\"#\" onclick=\"toggle(this); return false;\"><img src=\"../html/down-arrow.gif\" alt=\"X\"/><span style=\"padding-left:20px;color:green\">#{gname}</span></a></td></tr></thead>"
	else
		f.puts "<thead><tr><td colspan=\"6\"><a herf=\"#\" onclick=\"toggle(this); return false;\"><img src=\"../html/down-arrow.gif\" alt=\"X\"/><span style=\"padding-left:20px;color:red\">#{gname} : ERRORS (#{p_errors})</span></a></td></tr></thead>"
	end
	f.puts "</table>"
	f.puts "</td></tr>"


  f.puts "</tbody>"
  f.puts "</table>"
  f.puts "</body>"
  f.puts "</html>"
end
#puts "errors : #{error_count}
exit(error_count)
