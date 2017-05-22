import java.math.BigInteger
import java.security.NoSuchAlgorithmException
import java.util.Arrays
import java.security.MessageDigest


/**
 * Created by hexafraction on 5/20/17.
 */
//private fun DecodeBase58(input: String, base: Int, len: Int): ByteArray? {
//    val output = ByteArray(len)
//    for (i in 0..input.length - 1) {
//        val t = input[i]
//
//        var p = ALPHABET.indexOf(t)
//        if (p == -1) return null
//        var j = len - 1
//        while (j >= 0) {
//            p += base * (output[j].toInt() and 0xFF)
//            output[j] = (p % 256).toByte()
//            j--
//            p /= 256
//        }
//        if (p != 0) return null
//    }
//
//    return output
//}
fun main(args: Array<String>) {
    var kt = KeyTester()
    val monThread = Thread({
        while (true) {
            Thread.sleep(5000)
            kt.printStats()
        }
    })
    monThread.isDaemon = true

    println("Please input the private key: ")
    val knownPKey = readLine()!!

    /*JOptionPane.showInputDialog(
        null,
        "Please input private key:",
        "Typo recovery",
        JOptionPane.PLAIN_MESSAGE,
        null,
        null,
        "") as String
*/
    monThread.start()
    kt.tryKeys(knownPKey) { s -> kt.printIfNotSeen(s) }


}

class KeyTester {
    val fiftyEight = BigInteger.valueOf(58)
    val nullByte = List<Byte>(1, { 0.toByte() })
    @Volatile var decodes = 0L
    @Volatile var validDecodes = 0L
    @Volatile var startMillis = 0L


    fun printStats() {
        println("Tested $decodes keys; ${decodes / ((System.currentTimeMillis() - startMillis) / 1000.0).toInt()} keys per second")
    }

    private fun DecodeBase58(input: String): ByteArray? {
        var bi = BigInteger.ZERO

        for (i in 0..input.length - 1) {
            val t = input[i]

            val p = ALPHABET.indexOf(t)
            if (p == -1) return null
            bi *= fiftyEight
            bi += BigInteger.valueOf(p.toLong())
        }
        val ba = bi.toByteArray()
        return Arrays.copyOfRange(ba, 1, ba.size)
    }

    private fun EncodeBase58(input: ByteArray): String {
        var bi = BigInteger(input)
        var s = ""
        while (bi > BigInteger.ZERO) {
            s = ALPHABET[(bi % fiftyEight).toInt()] + s
            bi /= fiftyEight
        }
        return s
    }

    fun ValidateWIF(wif: String): Boolean {
        decodes++
        val decoded = DecodeBase58(wif) ?: return false
        //println(baToString(decoded))

        val decSize = decoded.size
        val data = Arrays.copyOfRange(decoded, 0, decSize-4)

        //println(baToString(data))
        val hash = Sha256(data, 2)!!
        if (hash[0] == decoded[decSize - 4]
                && hash[1] == decoded[decSize - 3]
                && hash[2] == decoded[decSize - 2]
                && hash[3] == decoded[decSize - 1]) {

            validDecodes++
            return true
        } else return false
//
//val decBlock = Arrays.copyOfRange(decoded, decoded.size - 4, decoded.size)
////println(baToString(decBlock))
//if(Arrays.equals(Arrays.copyOfRange(hash, 0, 4), decBlock)){
    }

    fun RebuildWIF(wif: String): String {
        val decoded = DecodeBase58(wif) ?: return ""
        val data = Arrays.copyOfRange(decoded, 0, decoded.size-4)
        val hash = Sha256(data, 2)
        return EncodeBase58((nullByte + data.toList() + Arrays.copyOfRange(hash, 0, 4).toList()).toByteArray())
    }

    private fun Sha256(data: ByteArray, rounds: Int): ByteArray? {
        if (rounds == 0) return data

        try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(data)
            val data1 = md.digest()
            //println(baToString(data1))
            return Sha256(data1, rounds - 1)
        } catch (e: NoSuchAlgorithmException) {
            return null
        }

    }

    // Debug use only
    private fun baToString(data: ByteArray) = Arrays.toString(data.map { b -> (b.toInt() and 0xFF).toString(16) }.toTypedArray())


    fun tryKeys(knownPKey: String, handleSolution: (String) -> Unit) {
        println("Note: This program will attempt to generate similar private keys to try to undo any typos.\nIt may generate multiple results, " +
                "some of which may have a zero balance.\nPlease try all of the resulting private keys by pasting them into bitaddress.org,\n" +
                "and checking the balance on the addresses.\n\n")
        /*JOptionPane.showMessageDialog(null,
            "Note: This program will attempt to generate similar private keys to try to undo any typos.\nIt may generate multiple results, " +
                    "some of which may have a zero balance.\nPlease try all of the resulting private keys by pasting them into bitaddress.org,\n" +
                    "and checking the balance on the addresses.\n\n");*/

        println(if (ValidateWIF(knownPKey)) "The key you input appears to be already valid." else "The key you input is invalid.")
        handleSolution(RebuildWIF(knownPKey))
        //println(knownPKey)
        startMillis = System.currentTimeMillis()
        println("Testing substitutions...")
        var subs1 = varySubstitutions(knownPKey)
        println("${subs1.size} substitutions generated.")
        for (s in subs1.filter { s -> ValidateWIF(s) }) {
            handleSolution(s)
        }

        //printStats()
        println("Testing transpositions...")
        val transpositions = varyTranspositions(knownPKey)
        println("${transpositions.size} transpositions generated.")
        for (s in transpositions.filter { s -> ValidateWIF(s) }) {
            handleSolution(s)
        }
        //printStats()
        println("Testing transpositions of substitutions...")
        for (s in subs1.flatMap { s -> varyTranspositions(s) }.filter { s -> ValidateWIF(s) }) {
            handleSolution(s)
        }
        //printStats()
        println("Testing consistently misread characters...")
        val vsa = varySubAll(knownPKey)

        println("${vsa.size} bulk substitutions generated.")
        for (s in vsa.stream().filter { s -> ValidateWIF(s) }) {
            handleSolution(s)
        }
        //printStats()
        println("Testing consistently misread characters with transposition...")
        for (s in vsa.stream().flatMap { s -> varyTranspositions(s).stream() }.filter { s -> ValidateWIF(s) }) {
            handleSolution(s)
        }
        //printStats()
        println("Testing double and triple transpositions...")
        for (s in transpositions.stream().flatMap { s -> varyTranspositions(s).stream() }.filter { s -> ValidateWIF(s) }) {
            handleSolution(s)
        }
        //printStats()
        for (s in transpositions.stream().flatMap { s -> varyTranspositions(s).stream() }.flatMap { s -> varyTranspositions(s).stream() }.filter { s -> ValidateWIF(s) }) {
            handleSolution(s)
        }
        //printStats()
        println("Warning: The following steps will take a while, due to a large search space.")
        println("Testing two substitutions...")
        for (s in subs1.stream().flatMap { s -> varySubstitutions(s).stream() }.filter { s -> ValidateWIF(s) }) {
            handleSolution(s)
        }
        println("Testing substitution and two transpositions...")
        for (s in subs1.stream().flatMap { s -> varyTranspositions(s).stream() }.flatMap { s -> varyTranspositions(s).stream() }.filter { s -> ValidateWIF(s) }) {
            handleSolution(s)
        }
        println("Testing bulk substitution followed by two transpositions...")
        ///for (s in vsa.stream().flatMap { s -> varyTranspositions(s).stream() }.flatMap { s -> varyTranspositions(s).stream() }.filter { s -> ValidateWIF(s) }) {
        //    printIfNotSeen(s)
        //}
        //printStats()
        println("Testing consistently misread characters with substitution...")
        for (s in vsa.stream().flatMap { s -> varySubstitutions(s).stream() }.filter { s -> ValidateWIF(s) }) {
            handleSolution(s)
        }
        //printStats()
        println("Testing consistently misread characters with substitution and transposition...")
        for (s in vsa.flatMap { s -> varySubstitutions(s) }.flatMap { s -> varyTranspositions(s) }.filter { s -> ValidateWIF(s) }) {
            handleSolution(s)
        }
        //printStats()
    }

    val seenWIFs = HashSet<String>()
    fun printIfNotSeen(s: String) {
        if (seenWIFs.contains(s)) return
        else {
            println(">>> Possible match: $s")

            //JOptionPane.showMessageDialog(null,
            //       "Found key: $s");
            seenWIFs.add(s)
        }

    }

    var calls: Int = 0
    fun varySubstitutions(base: String): List<String> {
        var l: MutableList<String> = MutableList(0, { i -> "" })
        for (i in 0..base.lastIndex) {
            for (c in ALPHABET) {
                l.add(base.substring(0..i - 1) + c + base.substring(i + 1..base.lastIndex))
            }
        }
        return l

    }

    fun varyTranspositions(base: String): List<String> {
        var l: MutableList<String> = MutableList(0, { i -> "" })
        for (i in 0..base.lastIndex - 1) {

            l.add(base.substring(0..i - 1) + base[i + 1] + base[i] + base.substring(i + 2..base.lastIndex))


        }
        return l
    }

    fun varySubAll(base: String): List<String> {
        var l: MutableList<String> = MutableList(0, { i -> "" })
        for (a in ALPHABET) {
            for (b in ALPHABET) {
                l.addAll(varySubAllInner(base, a, b, 0))
            }
        }
        return l
    }

    // recursively add all combinations of replacing and not replacing given character
    fun varySubAllInner(base: String, a: Char, b: Char, i: Int): List<String> {
        val idx = base.indexOf(a, i)
        if (idx == -1) {
            return listOf(base)
        } else {
            return varySubAllInner(base, a, b, idx + 1) + varySubAllInner(base.replaceRange(idx, idx + 1, b.toString()), a, b, idx + 1)
        }
    }

    val ALPHABET: String = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"
}