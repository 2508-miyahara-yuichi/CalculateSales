package jp.alhinc.calculate_sales;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalculateSales {

	// 支店定義ファイル名
	private static final String FILE_NAME_BRANCH_LST = "branch.lst";

	// 支店別集計ファイル名
	private static final String FILE_NAME_BRANCH_OUT = "branch.out";

	// エラーメッセージ
	private static final String UNKNOWN_ERROR = "予期せぬエラーが発生しました";
	private static final String FILE_NOT_EXIST = "支店定義ファイルが存在しません";
	private static final String FILE_INVALID_FORMAT = "支店定義ファイルのフォーマットが不正です";
	private static final String FILE_NOT_CONTINUOUS = "売上ファイル名が連番になっていません";
	private static final String TOTAL_AMOUNT_EXCESSIVE = "合計⾦額が10桁を超えました";
	private static final String FILE_FORMAT_INCORRECT = "のフォーマットが不正です";
	private static final String FILE_BRANCH_CODE_INCORRECT = "の支店コードが不正です";

	/**
	 * メインメソッド
	 *
	 * @param コマンドライン引数
	 */
	public static void main(String[] args) {
		// 支店コードと支店名を保持するMap
		Map<String, String> branchNames = new HashMap<>();
		// 支店コードと売上金額を保持するMap
		Map<String, Long> branchSales = new HashMap<>();

		if (args.length != 1) {
			System.out.println(UNKNOWN_ERROR);
			return;
		}

		// 支店定義ファイル読み込み処理
		if(!readFile(args[0], FILE_NAME_BRANCH_LST, branchNames, branchSales)) {
			return;
		}

		// ※ここから集計処理を作成してください。(処理内容2-1、2-2)
		File[] files = new File(args[0]).listFiles();


		List<File> rcdFiles = new ArrayList<>();

		for(int i = 0; i < files.length; i++) {    //指定したパスに存在するすべてのファイルの数だけこの処理は繰り返す
			String fileName = files[i].getName();		//取得したファイル名（i)をストリング型の変数fileNameに代入

			if((files[i].isFile()) && fileName.matches("^[0-9]{8}[.]rcd$")) {  //０～９の8桁かつ末尾が.rcdのファイルを読み込むための条件/売上ファイルかどうかの確認
				rcdFiles.add(files[i]);		//上記の条件に当てはまったもののみリストに追加
			}
		}

		Collections.sort(rcdFiles); ////リストを順番通り並べる（ソートする）

		for(int i = 0; i < rcdFiles.size() -1; i++) { 					//売上ファイルが連番か確認する(比較する回数はリストの数よりｰ1）
			int former = Integer.parseInt(rcdFiles.get(i).getName().substring(0, 8));	//ファイル名をint型に変換しつつ、ファイル名の8桁の数字を抽出する
			int latter = Integer.parseInt(rcdFiles.get(i + 1).getName().substring(0, 8)); 	//次のファイル名を（ファイル名＋１）で表現する

			if((latter - former) != 1) {
				System.out.println(FILE_NOT_CONTINUOUS);
				return;
			}

		}
		for(int i = 0; i < rcdFiles.size(); i++) {    //rcdファイルの数だけ処理を繰り返す
			String rcdFileName = rcdFiles.get(i).getName();	//rcdファイルのリストから名前を取り出す
			ArrayList<String> saleList = new ArrayList();	//リストに「０」支店コード「１」金額を格納する
			String fileCode = null;	//支店コードを代入するための変数を宣言
			BufferedReader br = null;

			try {
				File file = new File(args[0], rcdFileName);
				FileReader fr = new FileReader(file);		//newFile("ファイルのパス",ファイルの名前の変数）
				br = new BufferedReader(fr);
				String line;			//String型の変数lineを宣言する

				while((line = br.readLine()) != null) {	//一行ずつ読み込む
					saleList.add(line);			//変数line（String型）をsaleListに追加する
				}
				if(saleList.size() != 2) { //売上ファイルの中身が2行かどうか
					System.out.println(rcdFileName + FILE_FORMAT_INCORRECT);
					return;
				}

				if(!branchNames.containsKey(saleList.get(0))) {  //支店コードが支店定義ファイルに存在しているか
					System.out.println(rcdFileName + FILE_BRANCH_CODE_INCORRECT);
					return;
				}

				if(!saleList.get(1).matches("^[0-9]*$")) { //売上ファイルが数字か確認する
					System.out.println(UNKNOWN_ERROR);
					return;
				}

				fileCode = saleList.get(0);		//fileCodeに支店コードを代入する
				long fileSale = Long.parseLong(saleList.get(1));	//ストリング型のsaleListをlong型に変換
				Long saleAmount = branchSales.get(fileCode) + fileSale;	//マップに既にある数と読み込んだ数を足す

				if(saleAmount >= 10000000000L){		//売上金額が10桁を超えていないか確認
					System.out.println(TOTAL_AMOUNT_EXCESSIVE);	 //エラーメッセージ出力
					return;
				}
				branchSales.put(fileCode, saleAmount); 	//合算したものをマップに戻す

			}  catch(IOException e) {
				System.out.println(UNKNOWN_ERROR);

				return;

			} finally {
				// ファイルを開いている場合
				if(br != null) {
					try {
						// ファイルを閉じる
						br.close();

					} catch(IOException e) {
						System.out.println(UNKNOWN_ERROR);

						return;
					}
				}
			}

		}

		if(!writeFile(args[0], FILE_NAME_BRANCH_OUT, branchNames, branchSales)) {
			return;
		}

	}

	/**
	 * 支店定義ファイル読み込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 読み込み可否
	 */
	private static boolean readFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {

		BufferedReader br = null;

		try {
			File file = new File(path, fileName);
			if(!file.exists()) { 		//読み込もうとしたファイルが存在しない場合エラーメッセージを出力する
				System.out.println(FILE_NOT_EXIST);
				return false;
			}

			FileReader fr = new FileReader(file);
			br = new BufferedReader(fr);
			String line;
			// 一行ずつ読み込む

			while((line = br.readLine()) != null) {
				// ※ここの読み込み処理を変更してください。(処理内容1-2)
				String[] items=line.split(",");		//支店コードと支店名をそれぞれ別に保持するために文字列を分割する
				if((items.length != 2) || (!items[0].matches("^[0-9]{3}$"))){ 	//配列itemsにフォーマット通りの値が代入されているか確認/だめならエラーメッセージ
					System.out.println(FILE_INVALID_FORMAT);
					return false;
				}
				branchNames.put(items[0], items[1]);	//items(配列）の「０」（支店コード）、「１」支店名を取り出してブランチネームのマップに追加する
				branchSales.put(items[0], 0L );		//「０」（支店コード）と売上金額（まだ売り上げがないため０）をブランチセールスのマップに追加する

			}


		} catch(IOException e) {
			System.out.println(UNKNOWN_ERROR);

			return false;

		} finally {
			// ファイルを開いている場合
			if(br != null) {
				try {
					// ファイルを閉じる
					br.close();

				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);

					return false;
				}
			}
		}
		return true;
	}

	/**
	 * 支店別集計ファイル書き込み処理
	 *
	 * @param フォルダパス
	 * @param ファイル名
	 * @param 支店コードと支店名を保持するMap
	 * @param 支店コードと売上金額を保持するMap
	 * @return 書き込み可否
	 */
	private static boolean writeFile(String path, String fileName, Map<String, String> branchNames, Map<String, Long> branchSales) {
		// ※ここに書き込み処理を作成してください。(処理内容3-1)

		BufferedWriter bw = null;

		try {
			File file = new File(path, fileName);	//ファイル作成/ファイル書き込みの処理
			FileWriter fr = new FileWriter(file);
			bw = new BufferedWriter(fr);

			for(String key : branchNames.keySet()) {	//キー（支店コード）を利用してバリュー（支店名・売上金額）を抽出する
				String saleNumbers = Long.toString(branchSales.get(key)); 	//branchSalesがlong型でwriteメソッドを使用できないため
																			//branchSalesを変数に代入してString型に変換する
		    	bw.write(key + ",");	//支店コード＋”,”
				bw.write(branchNames.get(key) + ","); //支店名＋”,”
				bw.write(saleNumbers);	//最後に売上金額を追加する
				bw.newLine();

			}

		} catch(IOException e) {

			System.out.println(UNKNOWN_ERROR);

			return false;

		} finally {
			// ファイルを開いている場合
			if(bw != null) {
				try {
					// ファイルを閉じる
					bw.close();

				} catch(IOException e) {
					System.out.println(UNKNOWN_ERROR);

					return false;
				}
			}
		}


		return true;


	}
}

