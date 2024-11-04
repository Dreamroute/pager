package com.github.dreamroute.pager.starter.jsqlparser;

import static java.util.stream.Collectors.joining;

import java.util.List;
import lombok.SneakyThrows;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import org.junit.jupiter.api.Test;

class BaseTest {

    @Test
    void mm0() throws Exception {
        //        String sql = "SELECT o.*, g.* FROM test_order t LEFT JOIN test_goods tt ON t.id = tt.order_id WHERE
        // t.`code` = '100' AND tt.`name` = '小米11'";
        //        String sql = "select a.id, a.name, b.*, c.id, c.name from a left join b on a.id = b.aid left join c on
        // a.id = c.aid where a.name = 'w.dehai' and b.code = '10'";
        String sql =
                "SELECT o.*, g.id gid, g.`name` gname, g.order_id gorder_id FROM test_order o LEFT JOIN test_goods g ON o.id = g.order_id WHERE o.`code` = '100' AND g.order_id = 1";
        Select parse = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect body = (PlainSelect) parse.getSelectBody();
        List<SelectItem> selectItems = body.getSelectItems();
        String columns = selectItems.stream().map(Object::toString).collect(joining(","));
        FromItem fromItem = body.getFromItem();
        String joins = body.getJoins().stream().map(Object::toString).collect(joining(","));
        String result = "SELECT " + columns + " FROM " + fromItem.toString() + " " + joins + " ";
        System.err.println(result);
    }

    @Test
    @SneakyThrows
    void conditionTest() {
        String sql = "select * from smart_user where name = ? and password = ? or version > ?";
        Select parse = (Select) CCJSqlParserUtil.parse(sql);
        System.err.println("END");
    }

    @Test
    void mm() throws Exception {
        String sql =
                "SELECT T.*, tag.id as wechatGroupTagId, tag.name as wechatGroupTagName\n" + "FROM wechat_group T\n"
                        + "         left join (select * from wechat_group_tag where del_flag = 0) G\n"
                        + "                   on T.id = G.wechat_group_id\n"
                        + "         left join (select * from tag where del_flag = 0) tag on G.tag_id = tag.id\n"
                        + "where T.del_flag = 0\n"
                        + "order by T.id desc";
        Select parse = (Select) CCJSqlParserUtil.parse(sql);
        System.err.println(parse);
    }
}
