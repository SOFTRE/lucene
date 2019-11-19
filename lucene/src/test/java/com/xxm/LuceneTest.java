package com.xxm;

import com.xxm.dao.BookDaoImpl;
import com.xxm.pojo.Book;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.junit.Test;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 描述
 *
 * @author ljh
 * @version 1.0
 * @package com.xxm *
 * @Date 2019-11-16
 * @since 1.0
 */
public class LuceneTest {

    //索引的流程  add
    @Test
    public void createIndex() throws Exception {
        //1.采集数据 DB  JDBC

        BookDaoImpl bookDao = new BookDaoImpl();

        List<Book> books = bookDao.queryBookList();


        List<Document> documents = new ArrayList<Document>();

        for (Book book : books) {
            //2,创建文档
            Document document = new Document();

            //参数1 指定field 的名称 可以任意,一般使用列名
            //参数2 指定的field的名称对应的值
            //参数3 表示是否存储   是否存储 要看页面是否要显示. YES:要存储,NO:不存
            Field fieldid = new StringField("id", book.getId().toString(), Field.Store.YES);// 不分词  要索引  要存储
            Field fieldname = new TextField("name", book.getName(), Field.Store.YES);// 要分词,要索引,要存储
            Field fieldprice = new FloatField("price", book.getPrice(), Field.Store.YES);// 要分词,要索引,要存储
            Field fieldpic = new StoredField("pic", book.getPic());// 不分词,不索引,要存储
            Field fielddescription = new TextField("description", book.getDescription(), Field.Store.NO);// 要分词,要索引,不存储
            document.add(fieldid);
            document.add(fieldname);
            document.add(fieldprice);
            document.add(fieldpic);
            document.add(fielddescription);

            documents.add(document);
        }

        //3.分析文档(由分词器来自动的进行分析)
        //Analyzer analyzer = new StandardAnalyzer();//标准分词器
        Analyzer analyzer = new IKAnalyzer();

        //4.创建索引到索引库中存储即可
        //4.1 指定索引库的位置  可以存储到磁盘 也可以存储到内存中.选磁盘.
        Directory directory = FSDirectory.open(new File("G:\\index"));
        //4.2 创建索引写流(indexWriter)

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        //4.3 写入磁盘
        indexWriter.addDocuments(documents);
        indexWriter.commit();//提交

        //4.4 关闭流 finally
        indexWriter.close();
    }


    //搜索的流程  select
    @Test
    public void search() throws Exception {

        //1.用户需要在界面输入要搜索的文本  写死
        String text = "java";

        //2.创建查询对象 (解析文本 封装查询的语法) 用于执行搜索
        //name:java
        //参数1 指定要搜索的Field的名称
        //参数2 指定要搜索的内容文本
        Query query = new TermQuery(new Term("name", text));
        //3.创建执行器 去执行查询
        Directory directory = FSDirectory.open(new File("G:\\index"));
        //索引读 流
        IndexReader reader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(reader);


        //参数1 指定的要执行的查询对象(封装了语法的)
        //参数2 指定的查询到的至多排名靠前的数量
        TopDocs topDocs = indexSearcher.search(query, 100);
        //4.获取到结果集
        int totalHits = topDocs.totalHits;//总命中数
        System.out.println("总命中数:" + totalHits);
        //5.遍历结果集
        ScoreDoc[] scoreDocs = topDocs.scoreDocs;//

        for (ScoreDoc scoreDoc : scoreDocs) {
            int doc = scoreDoc.doc;//获取文档的ID

            //6.打印
            Document document = indexSearcher.doc(doc);
            System.out.println(document.get("id"));//指定的是FIled的名称
            System.out.println(document.get("name"));
        }

        reader.close();


    }

    //update  先删除,再添加. 如果没有数据,则直接添加.

    @Test
    public void updateDocument() throws Exception {
        //2.创建写流
        IndexWriter indexWirter = getIndexWirter();

        //3. 将name 为spring 更新为 spring update

        //3.1 创建一个更新后的文档
        Document documentupdated = new Document();
        documentupdated.add(new TextField("name", "spring update", Field.Store.YES));
        //3.2 执行更新

        //先根据name找内容为spring的文档,将这些文档更新为更新后的文档
        indexWirter.updateDocument(new Term("name", "abc"), documentupdated);

        indexWirter.commit();

        indexWirter.close();
    }


    private IndexWriter getIndexWirter() throws IOException {
        //1.指定索引库的位置
        //3.分析文档(由分词器来自动的进行分析)
        Analyzer analyzer = new StandardAnalyzer();//标准分词器

        //4.创建索引到索引库中存储即可
        //4.1 指定索引库的位置  可以存储到磁盘 也可以存储到内存中.选磁盘.
        Directory directory = FSDirectory.open(new File("G:\\index"));
        //4.2 创建索引写流(indexWriter)

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_10_3, analyzer);
        IndexWriter indexWriter = new IndexWriter(directory, config);

        return indexWriter;
    }


    //delete  根据查询到的结果删除,
    // delete 删除所有
    @Test
    public void delete()  throws Exception{
        IndexWriter indexWirter = getIndexWirter();
        //删除域名为name 内容为java文档
//        indexWirter.deleteDocuments(new Term("name","java"));
        indexWirter.deleteAll();// 全部删除.
        indexWirter.commit();
        indexWirter.close();
    }


    @Test
    public void testAnalyzer() throws IOException {
        //1.创建analyzer (ik)
        Analyzer analyzer = new IKAnalyzer();
        //2.获取tokenstream  (分的词都在此对象中)
        //第一个参数：就是域的名称，可以不写或者""
        //第二个参数：分析的词内容
        TokenStream tokenStream = analyzer.tokenStream("", "共产党是一个伟大的党啊");
        //3.指定一个引用   （指定 词的引用   或者 偏移量）
        CharTermAttribute addAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        //4.设置一个偏移量的引用
        OffsetAttribute offsetAttribute = tokenStream.addAttribute(OffsetAttribute.class);
        //5.调用tokenstream的rest方法 重置
        tokenStream.reset();
        //6.通过wihle 循环 遍历单词列表
        while(tokenStream.incrementToken()){
            ///打印
            System.out.println("start>>"+offsetAttribute.startOffset());
            System.out.println(addAttribute.toString());//打印单词
            System.out.println("end>>"+offsetAttribute.endOffset());
        }
        tokenStream.close();
        //关闭流
    }




}
