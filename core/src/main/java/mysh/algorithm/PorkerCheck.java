package mysh.algorithm;

import mysh.util.Strings;
import mysh.util.Tick;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PorkerCheck {
	public static void main(String[] args) throws IOException {
		int c = 0;
		List<String> lines = Files.readAllLines(Paths.get("l:/p054_poker.txt"));

		Tick tick = Tick.tick();
		for (String line : lines) {
			if (p1win(line)) c++;
		}

		System.out.println();
		System.out.println(tick.nip());
		System.out.println(c);
	}

	private static int[][] p1 = new int[5][2], p2 = new int[5][2];
	private static int[] cardCount1 = new int[15], cardCount2 = new int[15];
	private static int[] flowerCount1 = new int[5], flowerCount2 = new int[5];
	static Map<Integer, String> rules = new HashMap<>();

	static {
		fill(p1);
		fill(p2);
		rules.put(1, "High Card");
		rules.put(2, "One Pair");
		rules.put(3, "Two Pairs");
		rules.put(4, "Three of a Kind");
		rules.put(5, "Straight");
		rules.put(6, "Flush");
		rules.put(7, "Full House");
		rules.put(8, "Four of a Kind");
		rules.put(9, "Straight Flush");
		rules.put(10, "Royal Flush");
	}

	private static void fill(int[][] a) {
		for (int i = 0; i < a.length; i++) {
			a[i] = new int[2];
		}
	}

	/**
	 * check two players' porker card, each player has 5 cards, compare using common rules.
	 *
	 * @param line content like 9C JD 7C 6D TC 6H 6C JC 3D 3S
	 * @return whether player 1 wins.
	 */
	public static boolean p1win(String line) {
		if (Strings.isBlank(line)) return false;

		String[] cs = line.split(" ");
		fillPlayer(p1, cs, 0);
		fillPlayer(p2, cs, 5);

		Arrays.fill(cardCount1, 0);
		Arrays.fill(cardCount2, 0);
		Arrays.fill(flowerCount1, 0);
		Arrays.fill(flowerCount2, 0);
		for (int[] c : p1) {
			cardCount1[c[0]]++;
			flowerCount1[c[1]]++;
		}
		for (int[] c : p2) {
			cardCount2[c[0]]++;
			flowerCount2[c[1]]++;
		}

		System.out.println();
		System.out.println(line);
		int n = 10;
		while (n > 0) {
			int chkRule = chkRule(n);
			n--;
			if (chkRule == -2) continue;
			else if (chkRule == 0) {
				System.out.println(getRule(n + 1));
				return chkRule(1) > 0;
			} else {
				System.out.println(getRule(n + 1));
				System.out.println(chkRule > 0 ? "win" : "lose");
				return chkRule > 0;
			}
		}
		throw new IllegalStateException(line);
	}

	private static String getRule(int n) {
		return rules.get(n);
	}

	/**
	 * 1: player 1 win
	 * 0: tie
	 * -1: player 2 win
	 * -2: none ranked
	 */
	private static int chkRule(final int n) {
		int mp1, mp2;
		switch (n) {
			case 1: /*High Card: Highest value card.*/
				return highCard(cardCount1, cardCount2);
			case 2: /*One Pair: Two cards of the same value.*/
				mp1 = onePair(cardCount1); mp2 = onePair(cardCount2);
				return mp1 == mp2 ? (mp1 + mp2 < 0 ? -2 : 0) : (mp1 > mp2 ? 1 : -1);
			case 3: /*Two Pairs: Two different pairs.*/
				mp1 = twoPair(cardCount1); mp2 = twoPair(cardCount2);
				return mp1 == mp2 ? (mp1 + mp2 < 0 ? -2 : 0) : (mp1 > mp2 ? 1 : -1);
			case 4: /*Three of a Kind: Three cards of the same value.*/
				mp1 = threeCards(cardCount1); mp2 = threeCards(cardCount2);
				return mp1 == mp2 ? (mp1 + mp2 < 0 ? -2 : 0) : (mp1 > mp2 ? 1 : -1);
			case 5: /*Straight: All cards are consecutive values.*/
				mp1 = straight(cardCount1); mp2 = straight(cardCount2);
				return mp1 == mp2 ? (mp1 + mp2 < 0 ? -2 : 0) : (mp1 > mp2 ? 1 : -1);
			case 6: /*Flush: All cards of the same suit.*/
				mp1 = flush(flowerCount1); mp2 = flush(flowerCount2);
				return mp1 == mp2 ? (mp1 + mp2 < 0 ? -2 : 0) : (mp1 > mp2 ? 1 : -1);
			case 7: /*Full House: Three of a kind and a pair.*/
				mp1 = threeCards(cardCount1) * 100; mp2 = threeCards(cardCount2) * 100;
				int tp1 = onePair(cardCount1), tp2 = onePair(cardCount2);
				boolean f1 = mp1 > 0 && tp1 > 0, f2 = mp2 > 0 && tp2 > 0;
				if (f1 && f2) {
					mp1 += tp1; mp2 += tp2;
					return mp1 == mp2 ? (mp1 + mp2 < 0 ? -2 : 0) : (mp1 > mp2 ? 1 : -1);
				}
				if (f1) return 1;
				if (f2) return -1;
				return -2;
			case 8: /*Four of a Kind: Four cards of the same value.*/
				mp1 = fourCards(cardCount1); mp2 = fourCards(cardCount2);
				return mp1 == mp2 ? (mp1 + mp2 < 0 ? -2 : 0) : (mp1 > mp2 ? 1 : -1);
			case 9: /*Straight Flush: All cards are consecutive values of same suit.*/
				if (flush(flowerCount1) > 0 && flush(flowerCount2) > 0) {
					mp1 = straight(cardCount1);
					mp2 = straight(cardCount2);
					return mp1 == mp2 ? (mp1 + mp2 < 0 ? -2 : 0) : (mp1 > mp2 ? 1 : -1);
				}
				return -2;
			case 10:/*Royal Flush: Ten, Jack, Queen, King, Ace, in same suit.*/
				mp1 = royal(cardCount1) > 0 && flush(flowerCount1) > 0 ? 1 : -1;
				mp2 = royal(cardCount2) > 0 && flush(flowerCount2) > 0 ? 1 : -1;
				return mp1 == mp2 ? (mp1 + mp2 < 0 ? -2 : 0) : (mp1 > mp2 ? 1 : -1);
		}
		throw new IllegalStateException(n + "");
	}

	private static int royal(int[] c) {
		return c[10] == 1 && c[11] == 1 && c[12] == 1 && c[13] == 1 && c[14] == 1 ? 1 : -1;
	}

	private static int fourCards(int[] cardCount) {
		for (int i = cardCount.length - 1; i > 0; i--) {
			if (cardCount[i] == 4) return i;
		}
		return -1;
	}

	private static int flush(int[] f) {
		for (int fc : f) {
			if (fc == 5) return 1;
		}
		return -1;
	}

	private static int straight(int[] cardCount) {
		int c = 0, end = -1;
		for (int i = 0; i < cardCount.length; i++) {
			if (cardCount[i] == 1) {
				c++;
				end = i;
			} else if (c > 0 && c < 5)
				return -1;
		}
		return c == 5 ? end : -1;
	}

	private static int threeCards(int[] cardCount) {
		for (int i = cardCount.length - 1; i > 0; i--) {
			if (cardCount[i] == 3) return i;
		}
		return -1;
	}

	private static int twoPair(int[] cardCount) {
		int c = 0;
		for (int i = cardCount.length - 1; i > 0; i--) {
			if (cardCount[i] == 2) c = c * 100 + i;
		}
		return c < 100 ? -1 : c;
	}

	private static int onePair(int[] cardCount) {
		for (int i = cardCount.length - 1; i > 0; i--) {
			if (cardCount[i] == 2) return i;
		}
		return -1;
	}

	private static int highCard(int[] cc1, int[] cc2) {
		for (int i = cc1.length - 1; i > 0; i--) {
			if (cc1[i] > 0 && cc2[i] == 0) return 1;
			else if (cc1[i] == 0 && cc2[i] > 0) return -1;
		}
		throw new IllegalStateException(Arrays.toString(cc1) + " " + Arrays.toString(cc2));
	}

	private static void fillPlayer(int[][] p, String[] cs, final int ci) {
		for (int i = 0; i < 5; i++) {
			String c = cs[ci + i];
			switch (c.charAt(0)) {
				case 'T': p[i][0] = 10; break;
				case 'J': p[i][0] = 11; break;
				case 'Q': p[i][0] = 12; break;
				case 'K': p[i][0] = 13; break;
				case 'A': p[i][0] = 14; break;
				default: p[i][0] = c.charAt(0) - '0';
			}
			switch (c.charAt(1)) {
				case 'H': p[i][1] = 1; break;
				case 'D': p[i][1] = 2; break;
				case 'S': p[i][1] = 3; break;
				case 'C': p[i][1] = 4; break;
			}
		}
	}
}
