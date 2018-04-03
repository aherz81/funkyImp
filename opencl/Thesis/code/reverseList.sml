val aList = [1,2,3,4]
fun rev l = foldl nil (fn (a,b) => a::b) l
val reversed = rev aList (*Evaluates to [4,3,2,1]*)