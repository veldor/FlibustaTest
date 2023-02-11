package net.veldor.flibusta_test.model.adapter;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import net.veldor.flibusta_test.R;
import net.veldor.flibusta_test.model.selection.FilePojo;

import java.util.ArrayList;

public class FolderAdapter extends ArrayAdapter<FilePojo> {

	Activity context;
	ArrayList<FilePojo> dataList;

	public FolderAdapter(Activity context, ArrayList<FilePojo> dataList) {

		super(context, R.layout.fp_filerow, dataList);
		this.context = context;
		this.dataList = dataList;

	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		if(convertView == null){
			convertView = context.getLayoutInflater().inflate(R.layout.fp_filerow, parent, false);
		}

		TextView name = convertView.findViewById(R.id.name);
		ImageView icon = convertView.findViewById(R.id.itemType);

		if( dataList.get(position).isFolder() )
		{
			icon.setImageDrawable(
					ResourcesCompat.getDrawable(
							context.getResources(),
							R.drawable.ic_baseline_folder_24,
							context.getTheme()
					)
			);
		}
		else
		{
			String n = dataList.get(position).getName();
			if(n.endsWith("fb2")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.fb2_label,
								context.getTheme()
						)
				);
			}
			else if(n.endsWith("zip")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.zip_label,
								context.getTheme()
						)
				);
			}
			else if(n.endsWith("epub")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.epub_label,
								context.getTheme()
						)
				);
			}
			else if(n.endsWith("mobi")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.mobi_label,
								context.getTheme()
						)
				);
			}
			else if(n.endsWith("chm")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.chm_label,
								context.getTheme()
						)
				);
			}
			else if(n.endsWith("djvu")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.djvu_label,
								context.getTheme()
						)
				);
			}
			else if(n.endsWith("doc")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.doc_label,
								context.getTheme()
						)
				);
			}
			else if(n.endsWith("docx")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.docx_label,
								context.getTheme()
						)
				);
			}
			else if(n.endsWith("htm")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.htm_label,
								context.getTheme()
						)
				);
			}
			else if(n.endsWith("html")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.html_label,
								context.getTheme()
						)
				);
			}
			else if(n.endsWith("pdf")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.pdf_label,
								context.getTheme()
						)
				);
			}
			else if(n.endsWith("prc")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.prc_label,
								context.getTheme()
						)
				);
			}
			else if(n.endsWith("rtf")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.rtf_label,
								context.getTheme()
						)
				);
			}
			else if(n.endsWith("txt")){
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.txt_label,
								context.getTheme()
						)
				);
			}
			else{
				icon.setImageDrawable(
						ResourcesCompat.getDrawable(
								context.getResources(),
								R.drawable.misc_label,
								context.getTheme()
						)
				);
			}
		}

		name.setText( dataList.get(position).getName() );

		return convertView;
	}

	public void itemDeleted(int position) {
		dataList.remove(position);
		notifyDataSetChanged();
	}
}