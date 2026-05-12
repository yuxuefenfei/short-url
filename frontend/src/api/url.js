import request from "@/utils/request";

export const shortenUrl = (data) =>
  request({
    url: "/api/shorten",
    method: "post",
    data: {
      ...data,
      expiredTime: data.expiredTime ?? data.expiredAt ?? null,
    },
  });

export const getUrlStats = (shortKey) =>
  request({
    url: `/api/stats/${shortKey}`,
    method: "get",
  });
