-- 0007_views.sql
create view private.finance_monthly_summary as
select category_id, rt_id,
       private.wib_month(transaction_date) as bulan,
       sum(case when type = 'MASUK'  then amount else 0 end) as total_masuk,
       sum(case when type = 'KELUAR' then amount else 0 end) as total_keluar,
       sum(case when type = 'MASUK'  then amount else -amount end) as net_bulan_ini
from finances
where deleted_at is null
group by category_id, rt_id, private.wib_month(transaction_date);

-- Saldo kumulatif sejak pos berdiri
create view private.finance_monthly_recap as
select category_id, rt_id, bulan, total_masuk, total_keluar, net_bulan_ini,
       sum(net_bulan_ini) over (partition by category_id order by bulan) - net_bulan_ini as saldo_awal,
       sum(net_bulan_ini) over (partition by category_id order by bulan) as saldo_akhir
from private.finance_monthly_summary;
