export const tagMap = new Map();

export async function fetchTagLabels(qcodes) {
    if (!qcodes || qcodes.length === 0) return [];

    try {
        const results = await Promise.all(qcodes.map(async (qcode) => {
            const response = await fetch(`https://www.wikidata.org/w/api.php?action=wbgetentities&ids=${qcode}&format=json&languages=en&props=labels|descriptions&origin=*`);
            const data = await response.json();
            const entity = data.entities[qcode];

            const label = entity?.labels?.en?.value || "Unknown label";
            const description = entity?.descriptions?.en?.value || "No description";
            tagMap.set(qcode, label);

            return { id: qcode, label, description };
        }));

        return results;
    } catch (error) {
        console.error("Failed to fetch tag details:", error);
        return [];
    }
}
