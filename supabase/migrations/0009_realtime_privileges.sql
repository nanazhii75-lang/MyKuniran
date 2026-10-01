-- 0009_realtime_privileges.sql
alter publication supabase_realtime add table posts, finances, finance_categories, profiles;

-- Hanya RPC untuk client yang boleh dieksekusi role authenticated
revoke execute on all functions in schema public from public, anon, authenticated;
grant execute on function
  create_rt(text, text, text, text, text, text, text),
  preview_rt(text), request_join_rt(text), cancel_join_request(), list_pending_join_requests(),
  approve_join_request(uuid), reject_join_request(uuid), set_auto_approve(boolean),
  check_username_available(text), set_invite_username(text),
  update_rt_info(text, text, text, text, text, text),
  transfer_admin(uuid), assign_bendahara(uuid, uuid), leave_rt(), remove_member(uuid),
  pin_post(uuid), unpin_post(uuid), delete_post(uuid), delete_transaction(uuid),
  publish_finance_report(uuid, text), create_finance_agenda(uuid, text, timestamptz, text),
  publish_monthly_recap(uuid, date), register_device_token(text), unregister_device_token(text), rt_people()
to authenticated;
grant execute on function anonymize_account(uuid) to service_role;
