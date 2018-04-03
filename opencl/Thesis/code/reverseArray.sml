val arr = Array.fromList [1,2,3,4] (*{|1,2,3,4|}*)

fun reverse_array arr = 
	let 
		val arr_len = Array.length arr
		val arr_work = Array.array (arr_len, Array.sub (arr, 0)) (*1*)
		fun appi_op (p,v) = 	
			let
				val cur_pos = arr_len - 1 - p (*2*)
			in
				Array.sub (arr, cur_pos)
			end
		val x = Array.modifyi appi_op arr_work
	in
		arr_work
	end

val arr_rev = reverse_array arr (*Evaluates to {|4,3,2,1|}*)
