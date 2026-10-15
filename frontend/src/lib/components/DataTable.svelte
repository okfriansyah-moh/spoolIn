<script lang="ts">
  let {
    caption,
    rows,
  }: {
    caption: string;
    rows: { id: string; context: string; outcome: string }[];
  } = $props();
</script>

<table>
  <caption>{caption}</caption><thead
    ><tr
      ><th scope="col">Record</th><th scope="col">Context</th><th scope="col"
        >Outcome</th
      ></tr
    ></thead
  ><tbody
    >{#each rows as row (row.id)}<tr
        ><th scope="row">{row.id}</th><td>{row.context}</td><td
          >{row.outcome}</td
        ></tr
      >{/each}</tbody
  >
</table>
