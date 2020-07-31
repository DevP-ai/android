package mega.privacy.android.app.service.iab;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentSender;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.huawei.hmf.tasks.Task;
import com.huawei.hms.iap.Iap;
import com.huawei.hms.iap.IapApiException;
import com.huawei.hms.iap.IapClient;
import com.huawei.hms.iap.entity.InAppPurchaseData;
import com.huawei.hms.iap.entity.IsEnvReadyResult;
import com.huawei.hms.iap.entity.OrderStatusCode;
import com.huawei.hms.iap.entity.OwnedPurchasesReq;
import com.huawei.hms.iap.entity.OwnedPurchasesResult;
import com.huawei.hms.iap.entity.ProductInfo;
import com.huawei.hms.iap.entity.ProductInfoReq;
import com.huawei.hms.iap.entity.ProductInfoResult;
import com.huawei.hms.iap.entity.PurchaseIntentReq;
import com.huawei.hms.iap.entity.PurchaseIntentResult;
import com.huawei.hms.iap.entity.PurchaseResultInfo;
import com.huawei.hms.support.api.client.Status;

import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import mega.privacy.android.app.R;
import mega.privacy.android.app.middlelayer.iab.BillingManager;
import mega.privacy.android.app.middlelayer.iab.BillingUpdatesListener;
import mega.privacy.android.app.middlelayer.iab.MegaPurchase;
import mega.privacy.android.app.middlelayer.iab.MegaSku;
import mega.privacy.android.app.middlelayer.iab.QuerySkuListCallback;
import mega.privacy.android.app.utils.billing.Security;
import nz.mega.sdk.MegaApiJava;
import nz.mega.sdk.MegaUser;

import static mega.privacy.android.app.middlelayer.iab.BillingManager.RequestCode.REQ_CODE_BUY;
import static mega.privacy.android.app.utils.LogUtil.logDebug;
import static mega.privacy.android.app.utils.LogUtil.logError;
import static mega.privacy.android.app.utils.LogUtil.logWarning;

/**
 * Handles all the interactions with Play Store (via Billing library), maintains connection to
 * it through BillingClient and caches temporary states/data if needed
 */
public class BillingManagerImpl implements BillingManager {

    /** SKU for our subscription PRO_I monthly */
    public static final String SKU_PRO_I_MONTH = "mega.huawei.pro1.onemonth";

    /** SKU for our subscription PRO_I yearly */
    public static final String SKU_PRO_I_YEAR = "mega.huawei.pro1.oneyear";

    /** SKU for our subscription PRO_II monthly */
    public static final String SKU_PRO_II_MONTH = "mega.huawei.pro2.onemonth";

    /** SKU for our subscription PRO_II yearly */
    public static final String SKU_PRO_II_YEAR = "mega.huawei.pro2.oneyear";

    /** SKU for our subscription PRO_III monthly */
    public static final String SKU_PRO_III_MONTH = "mega.huawei.pro3.onemonth";

    /** SKU for our subscription PRO_III yearly */
    public static final String SKU_PRO_III_YEAR = "mega.huawei.pro3.oneyear";

    /** SKU for our subscription PRO_LITE monthly */
    public static final String SKU_PRO_LITE_MONTH = "mega.huawei.prolite.onemonth";

    /** SKU for our subscription PRO_LITE yearly */
    public static final String SKU_PRO_LITE_YEAR = "mega.huawei.prolite.oneyear";

    /** Public key for verify purchase. */
    private static final String PUBLIC_KEY = "MIIBojANBgkqhkiG9w0BAQEFAAOCAY8AMIIBigKCAYEA0xFr23QlccDUinSmbgDayePoxUCXxOtGzMgBPeB2EctW1v5m5nU4wUSDt2xMLtQVN6k+bGios/aphF+3fT3KWAlnjLgRyLwJ3G4MxmdXXjvI5OSp+8peCEQ/z3QFZ/+A5qssK88l9aRmobZcVH5q0X/H4G7nLZO4leXhXuBRNnHRTw6CNAv9tt9sjyKwMUOKJk9Ev+FpPYORcq/6Db1Q+hq9dsZOZImZAKSmmvdkdeczqvoZxVCD1EgWywfrLWZc8JjiaZHCRLFfMUkZ6SKObcv0/En97Rl7ih4tcW7FHo5ikKPgWy18nMg4/uZHO1biS6e2OyPL/XETiN3RuwR84qYg/FDmvdfqP9c4dt7z9WeYBia+4TjSpTdvUJl49qSKyrQkvk0z/HiafFbt9uiIDL+lHgU+944F8RWkQkogcZCJkUjuHihi/ZWQZitRmYNly+IQKc4d32hpgmaAtQoZHI92mABxmzWNDdnCF8nHFTl0g3FrL+WRsfd0eM/qtIknAgMBAAE=";
    public static final int PAY_METHOD_RES_ID = R.string.payment_method_huawei_wallet;
    public static final int PAY_METHOD_ICON_RES_ID = R.drawable.huawei_wallet_ic;
    public static final String SIGNATURE_ALGORITHM = "SHA256WithRSA";
    public static final int PAYMENT_GATEWAY = MegaApiJava.PAYMENT_METHOD_HUAWEI_WALLET;

    private String payload;

    private final Activity mActivity;

    private final BillingUpdatesListener mBillingUpdatesListener;

    private IapClient iapClient;

    private final List<MegaPurchase> mPurchases = new ArrayList<>();

    /**
     * Handles all the interactions with Play Store (via Billing library), maintains connection to
     * it through BillingClient and caches temporary states/data if needed.
     *
     * @param activity        The Context, here's {@link mega.privacy.android.app.lollipop.ManagerActivityLollipop}
     * @param updatesListener The callback, when billing status update. {@link BillingUpdatesListener}
     * @param pl              Payload, using MegaUser's hanlde as payload. {@link MegaUser#getHandle()}
     */
    public BillingManagerImpl(Activity activity, BillingUpdatesListener updatesListener, String pl) {
        mActivity = activity;
        mBillingUpdatesListener = updatesListener;
        payload = pl;

        iapClient = Iap.getIapClient(mActivity);
        Task<IsEnvReadyResult> task = iapClient.isEnvReady();
        task.addOnSuccessListener(result -> {
            logDebug("HMS IAP env is ready.");
            mBillingUpdatesListener.onBillingClientSetupFinished();
            queryPurchases();
        }).addOnFailureListener(this::handleException);
    }

    /**
     * Query purchases across various use cases and deliver the result in a formalized way through
     * a listener
     */
    public void queryPurchases() {
        getPurchase(false);
    }

    @Override
    public void updatePurchase() {
        getPurchase(true);
    }

    private void getPurchase(boolean refresh) {
        OwnedPurchasesReq req = new OwnedPurchasesReq();
        req.setPriceType(IapClient.PriceType.IN_APP_SUBSCRIPTION);
        req.setContinuationToken(null);

        Task<OwnedPurchasesResult> task = iapClient.obtainOwnedPurchases(req);
        task.addOnSuccessListener(result -> {
            mPurchases.clear();
            logDebug("obtainOwnedPurchases, success");
            List<String> inAppPurchaseDataList = result.getInAppPurchaseDataList();
            List<String> signatureList = result.getInAppSignature();

            for (int i = 0; i < signatureList.size(); i++) {
                String purchase = inAppPurchaseDataList.get(i);
                String signature = signatureList.get(i);

                // validate purchase's signature.
                boolean success = verifyValidSignature(purchase, signature);
                if (success) {
                    handlePurchase(purchase);
                }
            }
            if (refresh) {
                mBillingUpdatesListener.onPurchasesUpdated(false, result.getReturnCode(), mPurchases);
            } else {
                mBillingUpdatesListener.onQueryPurchasesFinished(false, result.getReturnCode(), mPurchases);
            }
        }).addOnFailureListener(this::handleException);
    }

    private void handlePurchase(String originalJson) {
        try {
            InAppPurchaseData data = new InAppPurchaseData(originalJson);
            if (data.getPurchaseState() == InAppPurchaseData.PurchaseState.PURCHASED && data.isSubValid()) {
                if (isPayloadValid(data.getDeveloperPayload())) {
                    logDebug("new purchase added, " + data.getDeveloperPayload());
                    mPurchases.add(Converter.convert(originalJson));
                } else {
                    logWarning("Invalid DeveloperPayload");
                }
            }
        } catch (JSONException e) {
            logError(e.getMessage(), e);
        }
    }

    /**
     * Verify signature of purchase.
     *
     * @param signedData the content of a purchase.
     * @param signature  the signature of a purchase.
     * @return If the purchase is valid.
     */
    @Override
    public boolean verifyValidSignature(String signedData, String signature) {
        try {
            return Security.verifyPurchase(signedData, signature, PUBLIC_KEY);
        } catch (IOException e) {
            logWarning("Purchase failed to valid signature", e);
            return false;
        }
    }

    @Override
    public boolean isPurchased(MegaPurchase purchase) {
        return purchase.getState() == InAppPurchaseData.PurchaseState.PURCHASED;
    }

    @Override
    public void initiatePurchaseFlow(@Nullable String oldSku, @Nullable String purchaseToken, @NonNull MegaSku skuDetails) {
        logDebug("oldSku is:" + oldSku + ", new sku is:" + skuDetails.getSku());

        //if user is upgrading, it take effect immediately otherwise wait until current plan expired
        //TODO
//        final int prorationMode = getProductLevel(skuDetails.getSku()) > getProductLevel(oldSku) ? 1 : 2;
//        logDebug("prorationMode is " + prorationMode);

        PurchaseIntentReq req = new PurchaseIntentReq();
        req.setPriceType(IapClient.PriceType.IN_APP_SUBSCRIPTION);
        req.setProductId(skuDetails.getSku());
        req.setDeveloperPayload(payload);

        Task<PurchaseIntentResult> task = iapClient.createPurchaseIntent(req);
        task.addOnSuccessListener(result -> {
            if (result == null) {
                logError("GetBuyIntentResult is null");
                return;
            }
            startResolutionForResult(result.getStatus(), REQ_CODE_BUY);
        }).addOnFailureListener(this::handleException);
    }

    @Override
    public void destroy() {
        // do nothing
    }

    @Override
    public int getPurchaseResult(Intent data) {
        PurchaseResultInfo purchaseResultInfo = iapClient.parsePurchaseResultInfoFromIntent(data);
        if (null == purchaseResultInfo) {
            return OrderStatusCode.ORDER_STATE_FAILED;
        }

        int returnCode = purchaseResultInfo.getReturnCode();
        switch (returnCode) {
            case OrderStatusCode.ORDER_PRODUCT_OWNED:
                return OrderStatusCode.ORDER_PRODUCT_OWNED;
            case OrderStatusCode.ORDER_STATE_SUCCESS:
                boolean credible = verifyValidSignature(purchaseResultInfo.getInAppPurchaseData(), purchaseResultInfo.getInAppDataSignature());
                if (credible) {
                    try {
                        InAppPurchaseData inAppPurchaseData = new InAppPurchaseData(purchaseResultInfo.getInAppPurchaseData());
                        if (inAppPurchaseData.isSubValid()) {
                            return OrderStatusCode.ORDER_STATE_SUCCESS;
                        }
                    } catch (JSONException e) {
                        return OrderStatusCode.ORDER_STATE_FAILED;
                    }
                }
                return OrderStatusCode.ORDER_STATE_FAILED;
            default:
                return returnCode;
        }
    }

    @Override
    public void getInventory(QuerySkuListCallback callback) {
        ProductInfoReq req = new ProductInfoReq();
        req.setPriceType(IapClient.PriceType.IN_APP_SUBSCRIPTION);
        req.setProductIds(IN_APP_SKUS);

        Task<ProductInfoResult> task = iapClient.obtainProductInfo(req);
        task.addOnSuccessListener(result -> {
            if (null == result) {
                logError("ProductInfoResult is null");
                return;
            }
            List<ProductInfo> productInfos = result.getProductInfoList();
            callback.onSuccess(Converter.convertSkus(productInfos));
        }).addOnFailureListener(this::handleException);
    }

    private void handleException(Exception e) {
        int code = -1;
        String message;
        if (e instanceof IapApiException) {
            IapApiException iapApiException = (IapApiException) e;
            logWarning("returnCode: " + iapApiException.getStatusCode());
            code = iapApiException.getStatusCode();
            switch (iapApiException.getStatusCode()) {
                case OrderStatusCode.ORDER_STATE_CANCEL:
                    message = code + " : Order has been canceled!";
                    break;
                case OrderStatusCode.ORDER_STATE_PARAM_ERROR:
                    message = code + " : Order state param error!";
                    break;
                case OrderStatusCode.ORDER_STATE_NET_ERROR:
                    message = code + " : Order state net error!";
                    break;
                case OrderStatusCode.ORDER_VR_UNINSTALL_ERROR:
                    message = code + " : Order vr uninstall error!";
                    break;
                case OrderStatusCode.ORDER_HWID_NOT_LOGIN:
                    startResolutionForResult(iapApiException.getStatus(), BillingManager.RequestCode.REQ_CODE_LOGIN);
                    message = code + " : Not login!";
                    break;
                case OrderStatusCode.ORDER_PRODUCT_OWNED:
                    message = code + " : Product already owned error!";
                    break;
                case OrderStatusCode.ORDER_PRODUCT_NOT_OWNED:
                    message = code + " : Product not owned error!";
                    break;
                case OrderStatusCode.ORDER_PRODUCT_CONSUMED:
                    message = code + " : Product consumed error!";
                    break;
                case OrderStatusCode.ORDER_ACCOUNT_AREA_NOT_SUPPORTED:
                    message = code + " : Order account area not supported error!";
                    break;
                case OrderStatusCode.ORDER_NOT_ACCEPT_AGREEMENT:
                    startResolutionForResult(iapApiException.getStatus(), BillingManager.RequestCode.REQ_CODE_BUYWITHPRICE_CONTINUE);
                    message = code + " : Not accepted!";
                    break;
                default:
                    // handle other error scenarios
                    message = code + " : Order unknown error!";
            }
            logError(message, e);
        } else {
            logError(e.getMessage(), e);
        }
    }

    /**
     * to start an activity.
     *
     * @param status  This parameter contains the pendingIntent object of the payment page.
     * @param reqCode Result code.
     */
    private void startResolutionForResult(Status status, int reqCode) {
        if (status == null) {
            logWarning("status is null");
            return;
        }
        if (status.hasResolution()) {
            try {
                status.startResolutionForResult(mActivity, reqCode);
            } catch (IntentSender.SendIntentException exp) {
                logError(exp.getMessage(), exp);
            }
        } else {
            logWarning("intent is null");
        }
    }

    @Override
    public boolean isPayloadValid(String pl) {
        return payload.equals(pl);
    }

    @Override
    public String getPayload() {
        return payload;
    }

    private static class Converter {

        public static MegaSku convert(ProductInfo sku) {
            MegaSku megaSku = new MegaSku();
            megaSku.setSku(sku.getProductId());
            return megaSku;
        }

        public static List<MegaSku> convertSkus(@Nullable List<ProductInfo> skus) {
            if (skus == null) {
                return null;
            }
            List<MegaSku> result = new ArrayList<>(skus.size());
            for (ProductInfo sku : skus) {
                result.add(convert(sku));
            }
            return result;
        }

        public static MegaPurchase convert(String originalJson) throws JSONException {
            InAppPurchaseData data = new InAppPurchaseData(originalJson);
            MegaPurchase purchase = new MegaPurchase();
            purchase.setUserHandle(data.getDeveloperPayload());
            purchase.setToken(data.getPurchaseToken());
            purchase.setState(data.getPurchaseState());
            purchase.setSku(data.getProductId());
            purchase.setReceipt(originalJson);
            return purchase;
        }

        public static List<MegaPurchase> convertPurchases(@Nullable List<String> originalJsons) {
            if (originalJsons == null) {
                return null;
            }
            List<MegaPurchase> result = new ArrayList<>(originalJsons.size());
            for (String data : originalJsons) {
                try {
                    result.add(convert(data));
                } catch (JSONException e) {
                    logError(e.getMessage(), e);
                }
            }
            return result;
        }
    }
}

