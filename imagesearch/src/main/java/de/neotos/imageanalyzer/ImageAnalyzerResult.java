/*
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * @author Torben Schinke <schinke[|@|]neotos.de> 
 */
package de.neotos.imageanalyzer;


public class ImageAnalyzerResult<USEROBJ> implements Comparable<ImageAnalyzerResult<USEROBJ>> {


	/**
	 * 与此结果关联的用户自定义对象.
	 */
	protected USEROBJ userObj;
	/**
	 * 匹配结果数量.
	 */
	protected int matches;
	/**
	 * 所有匹配结果特征值 与 给定特征值 距离和.
	 */
	protected double distSum;
	/**
	 * 所有匹配结果特征值 与 给定特征值 距离平均值.
	 */
	protected double distAvr = -1;
	/**
	 * 匹配程度, 越大匹配程度越高.
	 * 它必须是正值.
	 */
	protected double matchDegree = -1;

	protected ImageAnalyzerResult(USEROBJ userObj, int matches, double distSum) {
		this.userObj = userObj;
		this.matches = matches;
		this.distSum = distSum;
	}

	/**
	 * 计算匹配度. 需要更新 {@link #matchDegree}.
	 * 与 log10(匹配数) 成正比, 与特征值平均距离成反比.
	 */
	protected void calcMatchDegree() {
		matchDegree = matches < 2 ? 0 : Math.log10(matches) * matches / distSum;
	}

	/**
	 * 计算距离平均值. 需要更新 {@link #distAvr}.
	 */
	protected void calcDistAvr() {
		distAvr = distSum / matches;
	}


	/**
	 * 按匹配程度从大到小排列.
	 */
	@Override
	public int compareTo(ImageAnalyzerResult<USEROBJ> o) {
//		if (this.matchDegree < 0) this.calcMatchDegree();
//		if (o.matchDegree < 0) o.calcMatchDegree();
//		return o.matchDegree - this.matchDegree > 0 ? 1 : -1;

		if (matches != o.matches) return o.matches - matches;
		else {
			if (distAvr < 0) distAvr = distSum / matches;
			if (o.distAvr < 0) o.distAvr = o.distSum / o.matches;
			return distAvr - o.distAvr < 0 ? -1 : 1;
		}
	}

	@Override
	public String toString() {
		return "ImageAnalyzerResult{" +
						"userObj=" + userObj +
						", matches=" + matches +
						", distAvr=" + (distSum / matches) +
						", matchDegree=" + matchDegree +
						'}';
	}

	public USEROBJ getUserObj() {
		return userObj;
	}

	public int getMatches() {
		return matches;
	}
}
