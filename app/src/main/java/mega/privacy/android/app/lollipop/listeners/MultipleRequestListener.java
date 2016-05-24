package mega.privacy.android.app.lollipop.listeners;

import android.content.Context;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.ManagerActivityLollipop;
import mega.privacy.android.app.utils.Constants;
import mega.privacy.android.app.utils.Util;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaError;
import nz.mega.sdk.MegaRequest;
import nz.mega.sdk.MegaRequestListenerInterface;
import nz.mega.sdk.MegaShare;

//Listener for  multiselect
public class MultipleRequestListener implements MegaRequestListenerInterface {

    Context context;

    public MultipleRequestListener(int action, Context context) {
        super();
        this.actionListener = action;
        this.context = context;
    }

    int counter = 0;
    int error = 0;
    int max_items = 0;
    int actionListener = -1;
    String message;

    @Override
    public void onRequestUpdate(MegaApiJava api, MegaRequest request) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onRequestTemporaryError(MegaApiJava api, MegaRequest request, MegaError e) {

        log("Counter on onRequestTemporaryError: "+counter);
//			MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
//			if(node!=null){
//				log("onRequestTemporaryError: "+node.getName());
//			}
    }

    @Override
    public void onRequestStart(MegaApiJava api, MegaRequest request) {

        counter++;
        if(counter>max_items){
            max_items=counter;
        }
        log("Counter on RequestStart: "+counter);
    }

    @Override
    public void onRequestFinish(MegaApiJava api, MegaRequest request, MegaError e) {

        counter--;
        if (e.getErrorCode() != MegaError.API_OK){
            error++;
        }
        int requestType = request.getType();
        log("Counter on RequestFinish: "+counter);
        log("Error on RequestFinish: "+error);
//			MegaNode node = megaApi.getNodeByHandle(request.getNodeHandle());
//			if(node!=null){
//				log("onRequestTemporaryError: "+node.getName());
//			}
        if(counter==0){
            switch (requestType) {
                case  MegaRequest.TYPE_MOVE:{
                    if (actionListener== Constants.MULTIPLE_SEND_RUBBISH){
                        log("move to rubbish request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_moved_to_rubbish, max_items-error) + context.getString(R.string.number_incorrectly_moved_to_rubbish, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_moved_to_rubbish, max_items);
                        }
                    }
                    else{
                        log("move nodes request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_moved, max_items-error) + context.getString(R.string.number_incorrectly_moved, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_moved, max_items);
                        }
                    }
                    break;
                }
                case MegaRequest.TYPE_REMOVE:{
                    log("remove multi request finish");
                    if (actionListener==Constants.MULTIPLE_LEAVE_SHARE){
                        log("leave multi share");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_leaved, max_items-error) + context.getString(R.string.number_no_leaved, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_leaved, max_items);
                        }
                    }
                    else{
                        log("multi remove");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_removed, max_items-error) + context.getString(R.string.number_no_removed, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_removed, max_items);
                        }
                    }

                    break;
                }
                case MegaRequest.TYPE_REMOVE_CONTACT:{
                    log("multi contact remove request finish");
                    if(error>0){
                        message = context.getString(R.string.number_contact_removed, max_items-error) + context.getString(R.string.number_contact_not_removed, error);
                    }
                    else{
                        message = context.getString(R.string.number_contact_removed, max_items);
                    }
                    break;
                }
                case MegaRequest.TYPE_COPY:{
                    if (actionListener==Constants.MULTIPLE_CONTACTS_SEND_INBOX){
                        log("send to inbox multiple contacts request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_sent, max_items-error) + context.getString(R.string.number_no_sent, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_sent, max_items);
                        }
                    }
                    else if (actionListener==Constants.MULTIPLE_FILES_SEND_INBOX){
                        log("send to inbox multiple files request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_sent_multifile, max_items-error) + context.getString(R.string.number_no_sent_multifile, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_sent_multifile, max_items);
                        }
                    }
                    else{
                        log("copy request finished");
                        if(error>0){
                            message = context.getString(R.string.number_correctly_copied, max_items-error) + context.getString(R.string.number_no_copied, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_copied, max_items);
                        }
                    }
                    break;
                }
                case MegaRequest.TYPE_SHARE:{
                    log("multiple share request finished");
                    if(actionListener==Constants.MULTIPLE_REMOVE_SHARING_CONTACTS){
                        if(error>0){
                            message = context.getString(R.string.context_no_removed_sharing_contacts);
                        }
                        else{
                            message = context.getString(R.string.context_correctly_removed_sharing_contacts);
                        }
                    }
                    else if(actionListener==Constants.MULTIPLE_CONTACTS_SHARE){
                        //TODO change UI
                        //One file shared with many contacts
                        if(error>0){
                            message = context.getString(R.string.number_contact_file_shared_correctly, max_items-error) + context.getString(R.string.number_contact_file_not_shared_, error);
                        }
                        else{
                            message = context.getString(R.string.number_contact_file_shared_correctly, max_items);
                        }
                    }
                    else if(actionListener==Constants.MULTIPLE_FILE_SHARE){
                        //Many files shared with one contacts
                        if(error>0){
                            message = context.getString(R.string.number_correctly_shared, max_items-error) + context.getString(R.string.number_no_shared, error);
                        }
                        else{
                            message = context.getString(R.string.number_correctly_shared, max_items);
                        }
                    }
                    else{
                        if(error>0){
                            if(request.getAccess()== MegaShare.ACCESS_UNKNOWN){
                                message = context.getString(R.string.context_no_shared_number_removed, error);
                            }
                            else{
                                message = context.getString(R.string.context_no_shared_number, error);
                            }
                        }
                        else{
                            if(request.getAccess()==MegaShare.ACCESS_UNKNOWN){
                                message = context.getString(R.string.context_correctly_shared_removed);
                            }
                            else{
                                message = context.getString(R.string.context_correctly_shared);
                            }
                        }
                    }
                }
                default:
                    break;
            }
            ((ManagerActivityLollipop) context).showSnackbar(message);
        }
    }

    private static void log(String log) {
        Util.log("MultipleRequestListener", log);
    }
};
