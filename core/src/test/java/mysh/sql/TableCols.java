package mysh.sql;

public abstract class TableCols {
	public static class UmOrgCols {
		public final String orgId = "ORG_ID";
		public final String domainId = "DOMAIN_ID";
		public final String createTime = "CREATE_TIME";
		public final String email = "EMAIL";
	}
	public static final UmOrgCols UmOrgCols = new UmOrgCols();

	public static class UmOperatorCols {
		public final String operatorId = "OPERATOR_ID";
		public final String addressDetail = "ADDRESS_DETAIL";
		public final String positionCode = "POSITION_CODE";
	}
	public static final UmOperatorCols UmOperatorCols = new UmOperatorCols();

}
