
--! sqlRepoDemo.query
-- 每个 sql 配置语句由"--!"开头，后面跟 id
-- 文件名可以随便定, 建议 id 前缀与文件名一致, 方便查找
-- 全局只支持 单行注释 "--"
-- 使用中文输入法注意, 分隔符最好用英文空格和引号, 否则容易出现奇怪的问题
-- 建议用支持 sql 代码提示的 IDE 直接在这个文件里编辑, 比如 IntelliJ IDEA

select * from UM_OPERATOR --这里也可以写注释
where OPERATOR_ID=:id ; -- 语句结尾可以写分号, 也可以不写

